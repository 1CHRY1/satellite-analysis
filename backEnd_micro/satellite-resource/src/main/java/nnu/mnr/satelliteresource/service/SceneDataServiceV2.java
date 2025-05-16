package nnu.mnr.satelliteresource.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satelliteresource.model.dto.resources.ScenesFetchDTOV2;
import nnu.mnr.satelliteresource.model.po.Region;
import nnu.mnr.satelliteresource.model.po.Scene;
import nnu.mnr.satelliteresource.model.po.SceneSP;
import nnu.mnr.satelliteresource.model.vo.resources.SceneDesVO;
import nnu.mnr.satelliteresource.repository.ISceneRepo;
import nnu.mnr.satelliteresource.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
        return sceneRepo.selectById(sceneId);
    }

    public SceneSP getSceneByIdWithProductAndSensor(String sceneId) {
        return sceneRepo.getSceneByIdWithProductAndSensor(sceneId);
    }

    //    public List<SceneDesVO> getScenesDesByTimeRegionAndTag(ScenesFetchDTOV2 scenesFetchDTO) {
//        List<Scene> scenes = getScenesByTimeRegionAndCloud(scenesFetchDTO);
//        return sceneModelMapper.map(scenes, new TypeToken<List<SceneDesVO>>() {}.getType());
//    }
    public List<SceneDesVO> getScenesDesByTimeRegionAndTag(ScenesFetchDTOV2 scenesFetchDTO) {
        String startTime = scenesFetchDTO.getStartTime(); String endTime = scenesFetchDTO.getEndTime();
        Integer regionId = scenesFetchDTO.getRegionId(); Integer cloud = scenesFetchDTO.getCloud();
        Region region = regionDataService.getRegionById(regionId);
        String wkt = region.getBoundary().toText();
        return sceneRepo.getScenesDesByTimeCloudAndGeometry(startTime, endTime, cloud, wkt);
    }

    public List<Scene> getScenesByTimeRegionAndCloud(ScenesFetchDTOV2 scenesFetchDTO) {
        String startTime = scenesFetchDTO.getStartTime(); String endTime = scenesFetchDTO.getEndTime();
        Integer regionId = scenesFetchDTO.getRegionId(); Integer cloud = scenesFetchDTO.getCloud();
        Region region = regionDataService.getRegionById(regionId);
        Geometry regionBoundary = region.getBoundary();
        return sceneRepo.selectList(getQuaryByTimeCloudAndGeometry(startTime, endTime, cloud, regionBoundary));
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


}
