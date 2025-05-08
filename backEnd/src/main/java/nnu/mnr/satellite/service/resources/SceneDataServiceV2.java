package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTO;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV2;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.vo.resources.RegionInfoVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.repository.resources.ISceneRepo;
import nnu.mnr.satellite.utils.data.MinioUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:20
 * @Description:
 */

@Service("SceneDataServiceV2")
public class SceneDataServiceV2 {

    @Autowired
    private ModelMapper sceneModelMapper;

    @Autowired
    private RegionDataService regionDataService;

    private final ISceneRepo sceneRepo;

    public SceneDataServiceV2(ISceneRepo sceneRepo) {
        this.sceneRepo = sceneRepo;
    }

    public List<SceneDesVO> getScenesByTimeAndRegion(ScenesFetchDTOV2 scenesFetchDTO) {
        String startTime = scenesFetchDTO.getStartTime(); String endTime = scenesFetchDTO.getEndTime(); Integer regionId = scenesFetchDTO.getRegionId();
        Region region = regionDataService.getRegionById(regionId);
        Geometry regionBoundary = region.getBoundary();
        String wkt = regionBoundary.toText();
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("scene_time", startTime, endTime);
        queryWrapper.apply(
                "( ST_Intersects(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                        "ST_Contains(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                        "ST_Within(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) )",
                wkt
        );
        List<Scene> scenes = sceneRepo.selectList(queryWrapper);
        return sceneModelMapper.map(scenes, new TypeToken<List<SceneDesVO>>() {}.getType());
    }

    public JSONObject getSceneBoundaryById(String sceneId) throws IOException {
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scene_id", sceneId);
        Scene scene = sceneRepo.selectOne(queryWrapper);
        return GeometryUtil.geometry2Geojson(scene.getBbox());
    }

}
