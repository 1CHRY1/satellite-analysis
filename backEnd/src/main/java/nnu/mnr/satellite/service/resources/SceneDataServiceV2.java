package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.dto.resources.CoverFetchSceneDTO;
import nnu.mnr.satellite.model.dto.resources.RastersFetchDTO;
import nnu.mnr.satellite.model.dto.resources.SceneImageDTO;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV2;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.SceneSP;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.repository.resources.ISceneRepo;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    @Autowired
    private ImageDataService imageDataService;

    @Autowired
    BandMapperGenerator bandMapperGenerator;

    private final ISceneRepo sceneRepo;

    public SceneDataServiceV2(ISceneRepo sceneRepo) {
        this.sceneRepo = sceneRepo;
    }

    public Scene getSceneById(String sceneId) {
        return sceneRepo.selectById(sceneId);
    }

    public SceneImageDTO getSceneByIdWithImage(String sceneId) {
        SceneImageDTO sceneImageDTO = sceneRepo.getSceneWithImages(sceneId);
        JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(sceneImageDTO.getSensorName());
        sceneImageDTO.setBandMapper(bandMapper);
        return sceneImageDTO;
    }

    public SceneSP getSceneByIdWithProductAndSensor(String sceneId) {
        return sceneRepo.getSceneByIdWithProductAndSensor(sceneId);
    }

    // Get Scenes that Least Covering Region
    public List<ModelServerSceneDTO> getCoveredSceneByRegionResolutionAndSensor(CoverFetchSceneDTO coverFetchSceneDTO) {
        List<ModelServerSceneDTO> sceneDtos = new ArrayList<>();
        Geometry regionBoundary = regionDataService.getRegionById(coverFetchSceneDTO.getRegionId()).getBoundary();
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("scene_id", coverFetchSceneDTO.getSceneIds()).orderByDesc("scene_time");
        List<Scene> scenes = sceneRepo.selectList(queryWrapper);
        GeometryFactory geometryFactory = new GeometryFactory();
        MultiPolygon scenesBoundary = geometryFactory.createMultiPolygon(new Polygon[]{});
        for (Scene scene : scenes) {
            Geometry bbox = scene.getBbox();
            if (scenesBoundary.contains(bbox)) {
                continue;
            }
            if (bbox == null || bbox.isEmpty()) {
                throw new IllegalArgumentException("Invalid scene bounding box");
            }
            Geometry unionResult = scenesBoundary.union(bbox);
            if (unionResult instanceof MultiPolygon) {
                scenesBoundary = (MultiPolygon) unionResult;
            } else if (unionResult instanceof Polygon) {
                scenesBoundary = geometryFactory.createMultiPolygon(new Polygon[]{(Polygon) unionResult});
            } else {
                throw new IllegalArgumentException("Unsupported geometry type: " + unionResult.getClass().getName());
            }
            List<ModelServerImageDTO> imageDTOS = imageDataService.getModelServerImageDTOBySceneId(scene.getSceneId());
            ModelServerSceneDTO modelServerSceneDTO = ModelServerSceneDTO.builder()
                    .sceneId(scene.getSceneId())
                    .sceneTime(scene.getSceneTime())
                    .noData(scene.getNoData())
                    .bandMapper(bandMapperGenerator.getSatelliteConfigBySensorName(coverFetchSceneDTO.getSensorName()))
                    .images(imageDTOS)
                    .build();
            sceneDtos.add(modelServerSceneDTO);
            if (scenesBoundary.contains(regionBoundary)) {
                break;
            }
        }
        Collections.reverse(sceneDtos);
        return sceneDtos;
    }

    public List<SceneSP> getScenesByIdsWithProductAndSensor(List<String> sceneIds) {
        return sceneRepo.getScenesByIdsWithProductAndSensor(sceneIds);
    }

    public List<SceneDesVO> getScenesDesByTimeRegionAndCloud(ScenesFetchDTOV2 scenesFetchDTO) {
        String startTime = scenesFetchDTO.getStartTime(); String endTime = scenesFetchDTO.getEndTime();
        Integer regionId = scenesFetchDTO.getRegionId(); Integer cloud = scenesFetchDTO.getCloud();
        Region region = regionDataService.getRegionById(regionId);
        String wkt = region.getBoundary().toText();
        String dataType = "satellite";
        return sceneRepo.getScenesDesByTimeCloudAndGeometry(startTime, endTime, cloud, wkt, dataType);
    }

    public List<SceneDesVO> getRasterScenesDesByRegionAndDataType(RastersFetchDTO rastersFetchDTO) {
        String startTime = rastersFetchDTO.getStartTime(); String endTime = rastersFetchDTO.getEndTime();
        Integer regionId = rastersFetchDTO.getRegionId(); String dataType = rastersFetchDTO.getDataType();
        Region region = regionDataService.getRegionById(regionId);
        String wkt = region.getBoundary().toText();
        return sceneRepo.getScenesDesByTimeCloudAndGeometry(startTime, endTime, 100, wkt, dataType);
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