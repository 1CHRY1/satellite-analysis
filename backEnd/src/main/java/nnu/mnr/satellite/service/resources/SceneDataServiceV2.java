package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.dto.resources.*;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.SceneSP;
import nnu.mnr.satellite.model.vo.resources.SceneCoveredVO;
import nnu.mnr.satellite.model.vo.resources.ViewWindowVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.mapper.resources.ISceneRepo;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
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

    @Autowired
    LocationService locationService;

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

    public SceneCoveredVO getCoveredSceneByRegionResolutionAndSensor(CoverFetchSceneDTO coverFetchSceneDTO) {
        Geometry boundary = regionDataService.getRegionById(coverFetchSceneDTO.getRegionId()).getBoundary();
        List<String> sceneIds = coverFetchSceneDTO.getSceneIds(); String sensorName = coverFetchSceneDTO.getSensorName();
        Integer resolution = coverFetchSceneDTO.getResolution();
        return getCoveredSceneByBoundaryResolutionAndSensor(boundary, sceneIds, sensorName, resolution);
    } // For Region
    public SceneCoveredVO getCoveredSceneByLocationResolutionAndSensor(CoverLocationFetchSceneDTO CoverLocationFetchSceneDTO) {
        Geometry boundary = locationService.getLocationBoundary(CoverLocationFetchSceneDTO.getResolution(), CoverLocationFetchSceneDTO.getLocationId());
        List<String> sceneIds = CoverLocationFetchSceneDTO.getSceneIds(); String sensorName = CoverLocationFetchSceneDTO.getSensorName();
        Integer resolution = CoverLocationFetchSceneDTO.getResolution();
        return getCoveredSceneByBoundaryResolutionAndSensor(boundary, sceneIds, sensorName, resolution);
    } // For Location
    // Get Scenes that Least Covering Region
    public SceneCoveredVO getCoveredSceneByBoundaryResolutionAndSensor(Geometry boundary, List<String> sceneIds, String sensorName, Integer resolution) {
        List<ModelServerSceneDTO> sceneDtos = new ArrayList<>();
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("scene_id", sceneIds).orderByDesc("scene_time");

        // 获取格网边界
        List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
        Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
        JSONObject gridsBoundaryJSON = null;
        try {
            gridsBoundaryJSON = GeometryUtil.geometry2Geojson(gridsBoundary);
        } catch (IOException e) {
            // 处理异常（如记录日志或设置默认值）
            throw new RuntimeException("Failed to convert geometry to GeoJSON", e);
        }

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
            // 判断是否相交
            boolean isPartiallyOverlapped = isPartiallyOverlapped(gridsBoundary, bbox);

            List<ModelServerImageDTO> imageDTOS = imageDataService.getModelServerImageDTOBySceneId(scene.getSceneId());
            ModelServerSceneDTO modelServerSceneDTO = ModelServerSceneDTO.builder()
                    .sceneId(scene.getSceneId())
                    .sceneTime(scene.getSceneTime())
                    .noData(scene.getNoData())
                    .bandMapper(bandMapperGenerator.getSatelliteConfigBySensorName(sensorName))
                    .images(imageDTOS)
                    .isPartiallyOverlapped(isPartiallyOverlapped)
                    .build();
            sceneDtos.add(modelServerSceneDTO);
            if (scenesBoundary.contains(boundary)) {
                break;
            }
        }
//        Geometry interscet = regionBoundary.intersection(scenesBoundary);
//        Double res = interscet.getArea() / regionBoundary.getArea();
        Collections.reverse(sceneDtos);
        return SceneCoveredVO.builder()
                .sceneList(sceneDtos)
                .gridsBoundary(gridsBoundaryJSON)
                .build();
    }

    // 联合查询
    public List<SceneSP> getScenesByIdsWithProductAndSensor(List<String> sceneIds) {
        return sceneRepo.getScenesByIdsWithProductAndSensor(sceneIds);
    }
    // 数据库中求相交
//    public List<SceneSP> getScenesByIdsAndGridWithProductAndSensor(List<String> sceneIds, String wkt) {
//        return sceneRepo.getScenesByIdsAndGridWithProductAndSensor(sceneIds, wkt);
//    }

    public List<SceneDesVO> getScenesDesByTimeRegionAndCloud(ScenesFetchDTOV2 scenesFetchDTO) {
        String startTime = scenesFetchDTO.getStartTime(); String endTime = scenesFetchDTO.getEndTime();
        Integer regionId = scenesFetchDTO.getRegionId(); float cloud = scenesFetchDTO.getCloud();
        Integer resolution = scenesFetchDTO.getResolution();
        Geometry boundary = regionDataService.getRegionById(regionId).getBoundary();
        List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
        Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
        String wkt = gridsBoundary.toText();
        String dataType = "'satellite', 'dem', 'dsm', 'ndvi', 'svr', '3d'";
        return sceneRepo.getScenesDesByTimeCloudAndGeometry(startTime, endTime, cloud, wkt, dataType);
    }

    public List<SceneDesVO> getScenesDesByTimeLocationAndCloud(ScenesLocationFetchDTO scenesFetchDTO) {
        String startTime = scenesFetchDTO.getStartTime(); String endTime = scenesFetchDTO.getEndTime();
        float cloud = scenesFetchDTO.getCloud(); String locationId = scenesFetchDTO.getLocationId();
        Integer resolution = scenesFetchDTO.getResolution();
        Geometry boundary = locationService.getLocationBoundary(resolution, locationId);
        String wkt = boundary.toText();
        String dataType = "'satellite', 'dem', 'dsm', 'ndvi', 'svr', '3d'";
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
        Integer regionId = scenesFetchDTO.getRegionId(); float cloud = scenesFetchDTO.getCloud();
        Region region = regionDataService.getRegionById(regionId);
        Geometry regionBoundary = region.getBoundary();
        return sceneRepo.selectList(getQuaryByTimeCloudAndGeometry(startTime, endTime, cloud, regionBoundary));
    }
    // Get Scene Quary By Time And Geometry
    private QueryWrapper<Scene> getQuaryByTimeCloudAndGeometry(String startTime, String endTime, float cloud, Geometry geometry) {
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

    public ViewWindowVO getSceneWindowById(String sceneId) throws IOException {
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scene_id", sceneId);
        Scene scene = sceneRepo.selectOne(queryWrapper);
        return ViewWindowVO.builder()
                .center(List.of(scene.getBbox().getCentroid().getX(), scene.getBbox().getCentroid().getY()))
                .bounds(GeometryUtil.getGeometryBounds(scene.getBbox()))
                .build();
    }

    public static boolean isPartiallyOverlapped(Geometry geom1, Geometry geom2) {
    /**
     * 判断两个图形是否部分重叠（非完全包含或完全分离）
     */
        // 1. 检查是否相交
        if (!geom1.intersects(geom2)) {
            return false; // 完全分离
        }

        // 2. 排除完全包含的情况
        if (geom1.contains(geom2)) {
            return false; // 完全包含
        }

        // 3. 剩余情况即为部分重叠
        return true;
    }

}
