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
import java.util.stream.Collectors;

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

    public Scene getSceneById(String sceneId) {
        return sceneRepo.getSceneById(sceneId);
    }

    public List<SceneDesVO> getScenesDesByTimeRegionAndTag(ScenesFetchDTOV2 scenesFetchDTO) {
        List<Scene> scenes = getScenesByTimeRegionAndCloud(scenesFetchDTO);
        return sceneModelMapper.map(scenes, new TypeToken<List<SceneDesVO>>() {}.getType());
    }

    public List<Scene> getScenesByTimeRegionAndCloud(ScenesFetchDTOV2 scenesFetchDTO) {
        String startTime = scenesFetchDTO.getStartTime(); String endTime = scenesFetchDTO.getEndTime();
        Integer regionId = scenesFetchDTO.getRegionId(); Integer cloud = scenesFetchDTO.getCloud();
        Region region = regionDataService.getRegionById(regionId);
        Geometry regionBoundary = region.getBoundary();
        QueryWrapper<Scene> queryWrapper = getQuaryByTimeCloudAndGeometry(startTime, endTime, cloud, regionBoundary);
        return sceneRepo.selectList(queryWrapper);
    }

    public List<Scene> getScenesByTimeGeometryAndCloud(String startTime, String endTime, Integer cloud, Geometry geometry) {
        QueryWrapper<Scene> queryWrapper = getQuaryByTimeCloudAndGeometry(startTime, endTime, cloud, geometry);
        return sceneRepo.selectList(queryWrapper);
    }

    // Get Scene Quary By Time And Geometry
    private QueryWrapper<Scene> getQuaryByTimeCloudAndGeometry(String startTime, String endTime, Integer cloud, Geometry geometry) {
        String wkt = geometry.toText();
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("scene_time", startTime, endTime);
        queryWrapper.lt("cloud", cloud);
        queryWrapper.apply(
                "( ST_Intersects(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                        "ST_Contains(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                        "ST_Within(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) )",
                wkt
        );
        return queryWrapper;
    }

    public JSONObject getSceneBoundaryById(String sceneId) throws IOException {
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scene_id", sceneId);
        Scene scene = sceneRepo.selectOne(queryWrapper);
        return GeometryUtil.geometry2Geojson(scene.getBbox());
    }

    // Scene Tag Selector
    public List<Scene> getScenesByTag(List<Scene> scenes, JSONObject tags) {
        return scenes.stream()
                .filter(scene -> {
                    JSONObject sceneTags = scene.getTags();
//                    return tags.entrySet().stream().allMatch(tag ->
//                            sceneTags.containsKey(tag.getKey()) && sceneTags.get(tag.getKey()).equals(tag.getValue())
//                    );
                    return sceneTags == tags;
                })
                .collect(Collectors.toList());
    }

}
