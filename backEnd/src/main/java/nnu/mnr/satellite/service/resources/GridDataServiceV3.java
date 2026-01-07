package nnu.mnr.satellite.service.resources;

import lombok.Builder;
import lombok.Data;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.enums.common.SceneTypeByResolution;
import nnu.mnr.satellite.enums.common.SceneTypeByTheme;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.resources.GridBasicDTO;
import nnu.mnr.satellite.model.dto.resources.GridsWithFiltersDTO;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.GridBoundaryVO;
import nnu.mnr.satellite.model.vo.resources.GridsScenesOverlapVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static nnu.mnr.satellite.utils.geom.TileCalculateUtil.getTileGeomByIdsAndResolution;

@Service("GridDataServiceV3")
public class GridDataServiceV3 {

    @Autowired
    private ImageDataService imageDataService;

    @Autowired
    private BandMapperGenerator bandMapperGenerator;

    @Autowired
    private SceneDataServiceV3 sceneDataService;

    public CoverageReportVO<JSONObject> getScenesByGridAndResolution(GridBasicDTO gridBasicDTO, String cacheKey) throws IOException {
        Integer columnId = gridBasicDTO.getColumnId();
        Integer rowId = gridBasicDTO.getRowId();
        Integer resolution = gridBasicDTO.getResolution();
        Geometry wkt = getTileGeomByIdsAndResolution(rowId, columnId, resolution);
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        if (userSceneCache == null) {
            throw new NullPointerException("No corresponding cache found, please log in again or retrieve the data");
        }
        List<SceneDesVO> scenesInfo = userSceneCache.scenesInfo;
        if (scenesInfo == null || scenesInfo.isEmpty()) {
            return CoverageReportVO.<JSONObject>builder()
                    .total(0)
                    .coverage("0%")
                    .category(new ArrayList<>())
                    .dataset(null)
                    .build();
        }
        // 筛选出与格网相交的景
        List<SceneDesVO> filteredScenesInfo = scenesInfo.stream()
                .filter(scene -> {
                    // 计算覆盖度
                    AbstractMap.SimpleEntry<Double, Geometry> coverageResult =
                            sceneDataService.calculateCoveragePercentage(scene.getBoundingBox(), wkt);

                    // 条件：相交且覆盖度 > 20%
                    return coverageResult.getKey() > 0.2;
                })
                .toList();

        CoverageReportVO<JSONObject> report = new CoverageReportVO<>();
        List<String> category = SceneTypeByResolution.getCategoryNames();
        Integer total = filteredScenesInfo.size();
        report.setTotal(total); // 总数据量
        report.setCategory(category); // 分类名称列表

        Map<String, CoverageReportVO.DatasetItemVO<JSONObject>> dataset = new LinkedHashMap<>();
        for (SceneTypeByResolution type : SceneTypeByResolution.values()) {
            // 初始化每个分类的 DatasetItemVO
            CoverageReportVO.DatasetItemVO<JSONObject> item = new CoverageReportVO.DatasetItemVO<>();
            item.setLabel(type.getLabel());
            item.setResolution((float) type.getResolution());
            // 筛选当前分类的场景数据（根据 resolution 字段匹配）
            List<SceneDesVO> filteredScenes = filteredScenesInfo.stream()
                    .filter(scene -> sceneDataService.isSceneMatchResolutionType(scene, type))
                    .toList();
            item.setTotal(filteredScenes.size());
            // 构造dataList
            List<JSONObject> dataList = new ArrayList<>();
            if (!filteredScenes.isEmpty()) {
                for(SceneDesVO sceneInfo : filteredScenes){
                    JSONObject scene = new JSONObject();
                    scene.put("sceneId", sceneInfo.getSceneId());
                    scene.put("sceneTime", sceneInfo.getSceneTime());
                    scene.put("noData", sceneInfo.getNoData());
                    scene.put("sensorName", sceneInfo.getSensorName());
                    scene.put("platformName", sceneInfo.getPlatformName());
                    scene.put("productName", sceneInfo.getProductName());
                    scene.put("boundingBox", GeometryUtil.geometry2Geojson(sceneInfo.getBoundingBox()));
                    List<ModelServerImageDTO> images = imageDataService.getModelServerImageDTOBySceneId(sceneInfo.getSceneId());
                    scene.put("images", images);
                    JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(sceneInfo.getSensorName());
                    scene.put("bandMapper", bandMapper);
                    dataList.add(scene);
                }
            }
            // 排序
            List<JSONObject> sortedDataList = dataList.stream()
                    .sorted((scene1, scene2) -> {
                        // 按 sceneTime 降序排序（最新的在前）
                        LocalDateTime time1 = LocalDateTime.parse(scene1.getString("sceneTime"));
                        LocalDateTime time2 = LocalDateTime.parse(scene2.getString("sceneTime"));
                        return time2.compareTo(time1); // 降序
                    })
                    .toList(); // 重新收集成 List
            item.setDataList(sortedDataList);
            dataset.put(type.name(), item);
        }
        report.setDataset(dataset);
        return report;
    }

    public CoverageReportVO<JSONObject> getThemesByGridAndResolution(GridBasicDTO gridBasicDTO, String cacheKey) throws IOException {
        Integer columnId = gridBasicDTO.getColumnId();
        Integer rowId = gridBasicDTO.getRowId();
        Integer resolution = gridBasicDTO.getResolution();
        Geometry wkt = getTileGeomByIdsAndResolution(rowId, columnId, resolution);
        SceneDataCache.UserThemeCache userThemeCache = SceneDataCache.getUserThemeCacheMap(cacheKey);
        if (userThemeCache == null) {
            throw new NullPointerException("No corresponding cache found, please log in again or retrieve the data");
        }
        List<SceneDesVO> scenesInfo = userThemeCache.scenesInfo;
        if (scenesInfo == null || scenesInfo.isEmpty()) {
            return CoverageReportVO.<JSONObject>builder()
                    .total(0)
                    .coverage("0%")
                    .category(new ArrayList<>())
                    .dataset(null)
                    .build();
        }
        // 筛选出与格网相交的景
        List<SceneDesVO> filteredScenesInfo = scenesInfo.stream()
                .filter(scene -> isSceneIntersectGrid(scene, wkt))
                .toList();

        CoverageReportVO<JSONObject> report = new CoverageReportVO<>();
        List<String> category = SceneTypeByTheme.getAllCodes();
        Integer total = filteredScenesInfo.size();
        report.setTotal(total); // 总数据量
        report.setCategory(category); // 分类名称列表

        Map<String, CoverageReportVO.DatasetItemVO<JSONObject>> dataset = new LinkedHashMap<>();
        for (SceneTypeByTheme type : SceneTypeByTheme.values()) {
            // 初始化每个分类的 DatasetItemVO
            CoverageReportVO.DatasetItemVO<JSONObject> item = new CoverageReportVO.DatasetItemVO<>();
            item.setLabel(type.getLabel());
            // 筛选当前分类的场景数据（根据 datatype 字段分类）
            List<SceneDesVO> filteredScenes = scenesInfo.stream()
                    .filter(scene -> type.getCode().equalsIgnoreCase(scene.getDataType()))
                    .toList();
            item.setTotal(filteredScenes.size());
            // 构造dataList
            List<JSONObject> dataList = new ArrayList<>();
            if (!filteredScenes.isEmpty()) {
                for(SceneDesVO sceneInfo : filteredScenes){
                    JSONObject scene = new JSONObject();
                    scene.put("sceneId", sceneInfo.getSceneId());
                    scene.put("sceneTime", sceneInfo.getSceneTime());
                    scene.put("noData", sceneInfo.getNoData());
                    scene.put("sensorName", sceneInfo.getSensorName());
                    scene.put("productName", sceneInfo.getProductName());
                    scene.put("boundingBox", GeometryUtil.geometry2Geojson(sceneInfo.getBoundingBox()));
                    List<ModelServerImageDTO> images = imageDataService.getModelServerImageDTOBySceneId(sceneInfo.getSceneId());
                    scene.put("images", images);
                    JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(sceneInfo.getSensorName());
                    scene.put("bandMapper", bandMapper);
                    dataList.add(scene);
                }
            }
            // 排序
            List<JSONObject> sortedDataList = dataList.stream()
                    .sorted((scene1, scene2) -> {
                        // 按 sceneTime 降序排序（最新的在前）
                        LocalDateTime time1 = LocalDateTime.parse(scene1.getString("sceneTime"));
                        LocalDateTime time2 = LocalDateTime.parse(scene2.getString("sceneTime"));
                        return time2.compareTo(time1); // 降序
                    })
                    .toList(); // 重新收集成 List
            item.setDataList(sortedDataList);
            dataset.put(type.getCode(), item);
        }
        report.setDataset(dataset);
        return report;
    }

    // 数据准备模块
    public GridsScenesOverlapVO getScenesByGridsAndFilters(GridsWithFiltersDTO gridsWithFiltersDTO, String cacheKey){
        List<GridBasicDTO> gridsInfo = gridsWithFiltersDTO.getGrids();
        GridsWithFiltersDTO.SceneFilters filters = gridsWithFiltersDTO.getFilters();
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        if (userSceneCache == null) {
            throw new NullPointerException("No corresponding cache found, please log in again or retrieve the data");
        }
        List<SceneDesVO> scenesInfo = userSceneCache.scenesInfo;
        CoverageReportVO<Map<String, Object>> coverageReport = userSceneCache.coverageReportVO;

        if (scenesInfo == null || scenesInfo.isEmpty() || coverageReport == null) {
            return GridsScenesOverlapVO.builder()
                    .grids(new ArrayList<>())
                    .scenes(new ArrayList<>())
                    .build();
        }
        String resolutionName = filters.getResolutionName();
        List<String> sensorNames = new ArrayList<>();
        if (resolutionName != null) {
            List<Map<String, Object>> dataList = coverageReport.getDataset().get(resolutionName).getDataList();
            sensorNames = dataList.stream()
                    .map(map -> (String) map.get("sensorName")) // 提取 sensorName
                    .filter(Objects::nonNull) // 过滤掉 null 值（可选）
                    .toList();
        }
        List<SceneDesVO> filterScene = new ArrayList<>();
        for (SceneDesVO sceneInfo : scenesInfo) {
            JSONObject tag = sceneInfo.getTags();
            if ((sensorNames.contains(sceneInfo.getSensorName()) || resolutionName == null)
                    && (Objects.equals(filters.getSource(), tag.getString("source")) || filters.getSource() == null)
                    && (Objects.equals(filters.getProduction(), tag.getString("production")) || filters.getProduction() == null)
                    && (Objects.equals(filters.getCategory(), tag.getString("category")) || filters.getCategory() == null)) {
                filterScene.add(sceneInfo);
            }
        }

        List<GridsScenesOverlapVO.SceneInfo> scenes = new ArrayList<>();
        List<GridsScenesOverlapVO.GridsInfo> grids = new ArrayList<>();
        for (GridBasicDTO grid : gridsInfo) {
            boolean isOverLapped = false;
            Geometry bbox = getTileGeomByIdsAndResolution(grid.getRowId(), grid.getColumnId(), grid.getResolution());
            for (SceneDesVO scene : filterScene) {
                if (scene.getBoundingBox().contains(bbox)) {
                    isOverLapped = true;
                    GridsScenesOverlapVO.SceneInfo sceneInfo = GridsScenesOverlapVO.SceneInfo.builder()
                            .sceneId(scene.getSceneId())
                            .platformName(scene.getPlatformName())
                            .build();
                    if(!scenes.contains(sceneInfo)){
                        scenes.add(sceneInfo);
                    }
                }
            }
            GridsScenesOverlapVO.GridsInfo gridInfo = GridsScenesOverlapVO.GridsInfo.builder()
                    .columnId(grid.getColumnId())
                    .rowId(grid.getRowId())
                    .resolution(grid.getResolution())
                    .isOverlapped(isOverLapped)
                    .build();
            grids.add(gridInfo);
        }

        return GridsScenesOverlapVO.builder()
                .grids(grids)
                .scenes(scenes)
                .build();
    }

    public GridBoundaryVO getBoundaryByResolutionAndId(GridBasicDTO gridBasicDTO) throws IOException {
        Integer rowId = gridBasicDTO.getRowId();
        Integer columnId = gridBasicDTO.getColumnId();
        Integer resolution = gridBasicDTO.getResolution();
        return TileCalculateUtil.getTileBoundaryByIdsAndResolution(rowId, columnId, resolution);
    }

    private boolean isSceneIntersectGrid(SceneDesVO scene, Geometry wkt){
        return wkt.intersects(scene.getBoundingBox());
    }

}
