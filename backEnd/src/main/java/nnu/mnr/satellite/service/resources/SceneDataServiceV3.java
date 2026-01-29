package nnu.mnr.satellite.service.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.enums.common.SceneTypeByResolution;
import nnu.mnr.satellite.enums.common.SceneTypeByTheme;
import nnu.mnr.satellite.mapper.resources.ISceneRepoV3;
import nnu.mnr.satellite.model.dto.cache.CacheDataDTO;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV3;
import nnu.mnr.satellite.model.dto.resources.ScenesLocationFetchDTOV3;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportWithCacheKeyVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service("SceneDataServiceV3")
public class SceneDataServiceV3 {

    @Autowired
    private RegionDataService regionDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationService locationService;

    private ISceneRepoV3 sceneRepo;

    public SceneDataServiceV3(ISceneRepoV3 sceneRepo) {
        this.sceneRepo = sceneRepo;
    }

    public CoverageReportWithCacheKeyVO<Map<String, Object>> getScenesCoverageReportByTimeAndRegion(ScenesFetchDTOV3 scenesFetchDTO, String userId){
        // 先把scenesFetchDTO转成String，后续需要作为cacheKey
        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(scenesFetchDTO);
        } catch (JsonProcessingException e) {
            // 记录日志并返回默认值或抛出运行时异常
            log.error("Failed to serialize ScenesFetchDTOV2 to JSON", e);
            throw new RuntimeException("Invalid request data", e); // 或返回默认值
        }
        // 生成cacheKey，由userId和requestBody共同生成
        String encryptedRequestBody = DigestUtils.sha256Hex(requestBody);
        String cacheKey = userId + "_" + encryptedRequestBody;
        // 从缓存读取数据（如果存在）
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        SceneDataCache.UserThemeCache userThemeCache = SceneDataCache.getUserThemeCacheMap(cacheKey);
        LocalDateTime startTime = scenesFetchDTO.getStartTime();
        LocalDateTime endTime = scenesFetchDTO.getEndTime();
        Integer regionId = scenesFetchDTO.getRegionId();
        Integer resolution = scenesFetchDTO.getResolution();
        CoverageReportVO<Map<String, Object>> report;
        List<String> dataType = new ArrayList<>();
        if (userSceneCache == null && userThemeCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry boundary = regionDataService.getRegionById(regionId).getBoundary();
            List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
            Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
            List<String> themeCodes = SceneTypeByTheme.getAllCodes();
            dataType.add("satellite");
            dataType.addAll(themeCodes);
            List<SceneDesVO> allScenesInfo = queryAndCleanScenes(startTime, endTime, gridsBoundary, dataType);
            List<SceneDesVO> scenesInfo = new ArrayList<>();
            List<SceneDesVO> themesInfo = new ArrayList<>();
            for (SceneDesVO scene : allScenesInfo) {
                String sceneDataType = scene.getDataType();
                if ("satellite".equals(sceneDataType)) {
                    scenesInfo.add(scene);
                } else if (SceneTypeByTheme.getAllCodes().contains(sceneDataType)) {
                    themesInfo.add(scene);
                }
            }
            // 计算时间
            long startCalTime = System.currentTimeMillis();
            report = buildCoverageReport(scenesInfo, gridsBoundary);
            long endCalTime = System.currentTimeMillis(); // 记录结束时间
            long duration = endCalTime - startCalTime; // 计算耗时（毫秒）
            System.out.println("在内存中分类所消耗时间: " + duration + "ms");
            // 缓存数据
            SceneDataCache.cacheUserScenes(cacheKey, scenesInfo, report);
            SceneDataCache.cacheUserThemes(cacheKey, themesInfo, null);
            // 格网边界缓存
            SceneDataCache.cacheUserRegionInfo(cacheKey, gridsBoundary, startTime, endTime);
        } else if (userSceneCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry boundary = regionDataService.getRegionById(regionId).getBoundary();
            List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
            Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
            dataType.add("satellite");
            List<SceneDesVO> scenesInfo = queryAndCleanScenes(startTime, endTime, gridsBoundary, dataType);

            report = buildCoverageReport(scenesInfo, gridsBoundary);

            // 缓存数据
            SceneDataCache.cacheUserScenes(cacheKey, scenesInfo, report);
            // 格网边界缓存
            SceneDataCache.cacheUserRegionInfo(cacheKey, gridsBoundary, startTime, endTime);
        } else if (userSceneCache.coverageReportVO == null) {
            Geometry boundary = regionDataService.getRegionById(regionId).getBoundary();
            List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
            Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
            report = buildCoverageReport(userSceneCache.scenesInfo, gridsBoundary);
            // 缓存数据
            SceneDataCache.cacheUserScenes(cacheKey, userSceneCache.scenesInfo, report);
            // 格网边界缓存
            SceneDataCache.cacheUserRegionInfo(cacheKey, gridsBoundary, startTime, endTime);
        } else {
            // 缓存命中，直接使用
            report = userSceneCache.coverageReportVO;
        }
        CoverageReportWithCacheKeyVO<Map<String, Object>> result = new CoverageReportWithCacheKeyVO<>();
        result.setReport(report);
        result.setEncryptedRequestBody(encryptedRequestBody); // 返回给 Controller 设置 Cookie
        return result;
    }

    public CoverageReportWithCacheKeyVO<Map<String, Object>> getScenesCoverageReportByTimeAndLocation(ScenesLocationFetchDTOV3 scenesLocationFetchDTO, String userId){
        // 先把scenesFetchDTO转成String，后续需要作为cacheKey
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(scenesLocationFetchDTO);
        } catch (JsonProcessingException e) {
            // 记录日志并返回默认值或抛出运行时异常
            log.error("Failed to serialize ScenesFetchDTOV2 to JSON", e);
            throw new RuntimeException("Invalid request data", e); // 或返回默认值
        }
        // 生成cacheKey，由userId和requestBody共同生成
        String encryptedRequestBody = DigestUtils.sha256Hex(requestBody);
        String cacheKey = userId + "_" + encryptedRequestBody;
        // 从缓存读取数据（如果存在）
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        SceneDataCache.UserThemeCache userThemeCache = SceneDataCache.getUserThemeCacheMap(cacheKey);
        LocalDateTime startTime = scenesLocationFetchDTO.getStartTime();
        LocalDateTime endTime = scenesLocationFetchDTO.getEndTime();
        String locationId = scenesLocationFetchDTO.getLocationId();
        Integer resolution = scenesLocationFetchDTO.getResolution();
        CoverageReportVO<Map<String, Object>> report;
        List<String> dataType = new ArrayList<>();
        if (userSceneCache == null && userThemeCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry gridsBoundary = locationService.getLocationBoundary(resolution, locationId);
            List<String> themeCodes = SceneTypeByTheme.getAllCodes();
            dataType.add("satellite");
            dataType.addAll(themeCodes);
            List<SceneDesVO> allScenesInfo = queryAndCleanScenes(startTime, endTime, gridsBoundary, dataType);
            List<SceneDesVO> scenesInfo = new ArrayList<>();
            List<SceneDesVO> themesInfo = new ArrayList<>();
            for (SceneDesVO scene : allScenesInfo) {
                String sceneDataType = scene.getDataType(); // 假设 SceneDesVO 有 getDataType() 方法
                if ("satellite".equals(sceneDataType)) {
                    scenesInfo.add(scene);
                } else if (SceneTypeByTheme.getAllCodes().contains(sceneDataType)) {
                    themesInfo.add(scene);
                }
            }

            report = buildCoverageReport(scenesInfo, gridsBoundary);
            // 缓存数据
            SceneDataCache.cacheUserScenes(cacheKey, scenesInfo, report);
            SceneDataCache.cacheUserThemes(cacheKey, themesInfo, null);
            // 格网边界缓存
            SceneDataCache.cacheUserRegionInfo(cacheKey, gridsBoundary, startTime, endTime);
        } else if (userSceneCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry gridsBoundary = locationService.getLocationBoundary(resolution, locationId);
            dataType.add("satellite");
            List<SceneDesVO> scenesInfo = queryAndCleanScenes(startTime, endTime, gridsBoundary, dataType);

            report = buildCoverageReport(scenesInfo, gridsBoundary);
            // 缓存数据
            SceneDataCache.cacheUserScenes(cacheKey, scenesInfo, report);
            // 格网边界缓存
            SceneDataCache.cacheUserRegionInfo(cacheKey, gridsBoundary, startTime, endTime);
        }else if (userSceneCache.coverageReportVO == null) {
            Geometry gridsBoundary = locationService.getLocationBoundary(resolution, locationId);
            report = buildCoverageReport(userSceneCache.scenesInfo, gridsBoundary);
            // 缓存数据
            SceneDataCache.cacheUserScenes(cacheKey, userSceneCache.scenesInfo, report);
            // 格网边界缓存
            SceneDataCache.cacheUserRegionInfo(cacheKey, gridsBoundary, startTime, endTime);
        } else {
            // 缓存命中，直接使用
            report = userSceneCache.coverageReportVO;
        }
        CoverageReportWithCacheKeyVO<Map<String, Object>> result = new CoverageReportWithCacheKeyVO<>();
        result.setReport(report);
        result.setEncryptedRequestBody(encryptedRequestBody); // 返回给 Controller 设置 Cookie
        return result;
    }

    public List<SceneDesVO> getScenesByTimeAndRegion(LocalDateTime startTime, LocalDateTime endTime, Geometry boundary, List<String> dataType) {
        String wkt = boundary.toText();
        return sceneRepo.getScenesInfoByTimeAndRegion(startTime, endTime, wkt, dataType);
    }

    private CoverageReportVO<Map<String, Object>> buildCoverageReport(List<SceneDesVO> scenesInfo, Geometry gridsBoundary){
        CoverageReportVO<Map<String, Object>> report = new CoverageReportVO<>();

        Integer total = scenesInfo.size();
        // 计算覆盖度
//        double coverageRatio = calculateCoverageRatio(scenesInfo, gridsBoundary) * 100;
//        String coverage = String.format("%.2f%%", coverageRatio);
        List<String> category = SceneTypeByResolution.getCategoryNames();
        // 构建返回结果
        report.setTotal(total); // 总数据量
//        report.setCoverage(coverage); // 总体覆盖率
        report.setCategory(category); // 分类名称列表

        // 按分辨率分类统计
        Map<String, CoverageReportVO.DatasetItemVO<Map<String, Object>>> dataset = new LinkedHashMap<>();
        for (SceneTypeByResolution type : SceneTypeByResolution.values()) {
            // 初始化每个分类的 DatasetItemVO
            CoverageReportVO.DatasetItemVO<Map<String, Object>> item = new CoverageReportVO.DatasetItemVO<>();
            item.setLabel(type.getLabel());
            item.setResolution((float) type.getResolution());

            // 筛选当前分类的场景数据（根据 resolution 字段匹配）
            List<SceneDesVO> filteredScenes = scenesInfo.stream()
                    .filter(scene -> isSceneMatchResolutionType(scene, type))
                    .collect(Collectors.toList());

            item.setTotal(filteredScenes.size());
//            coverageRatio = calculateCoverageRatio(filteredScenes, gridsBoundary) * 100;
//            item.setCoverage(String.format("%.2f%%", coverageRatio));
            Set<String> seen = new HashSet<>();
            List<Map<String, Object>> uniqueDataList = filteredScenes.stream()
                    .filter(scene -> seen.add(scene.getSensorName() + "|" + scene.getPlatformName())) // 利用 Set 去重
                    .map(scene -> {
                        Map<String, Object> sensorInfo = new HashMap<>();
                        sensorInfo.put("sensorName", scene.getSensorName());
                        sensorInfo.put("platformName", scene.getPlatformName());
                        return sensorInfo;
                    })
                    .collect(Collectors.toList());

            item.setDataList(uniqueDataList);
            dataset.put(type.name(), item);
        }
        report.setDataset(dataset);
        return report;
    }

    public CommonResultVO getCoverageByCategory(String category, String cacheKey){
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        SceneDataCache.UserRegionInfoCache userRegionInfoCache = SceneDataCache.getUserRegionInfoCacheMap(cacheKey);
        if (userSceneCache!= null && userRegionInfoCache != null) {
            List<SceneDesVO> scenesInfo = userSceneCache.scenesInfo;
            Geometry gridsBoundary = userRegionInfoCache.gridsBoundary;
            if (scenesInfo != null && scenesInfo.size() > 0 && gridsBoundary != null) {
                if (category == null || category.isEmpty() || category.equals("all")) {
                    double coverageRatio = calculateCoverageRatio(scenesInfo, gridsBoundary) * 100;
                    String coverage = String.format("%.2f%%", coverageRatio);
                    return CommonResultVO.builder().status(1).message("覆盖度计算完成").data(coverage).build();
                } else {
                    List<SceneDesVO> filteredScenes = scenesInfo.stream()
                            .filter(scene -> isSceneMatchResolutionType(scene, category))
                            .collect(Collectors.toList());
                    double coverageRatio = calculateCoverageRatio(filteredScenes, gridsBoundary) * 100;
                    String coverage = String.format("%.2f%%", coverageRatio);
                    return CommonResultVO.builder().status(1).message("覆盖度计算完成").data(coverage).build();
                }
            } else {
                return CommonResultVO.builder().status(-1).message("景为空或者格网边界不存在").build();
            }

        } else {
            return CommonResultVO.builder().status(-1).message("缓存不存在").build();
        }
    }

     // 计算并集后调用覆盖度函数
    public double calculateCoverageRatio(List<SceneDesVO> scenesInfo, Geometry gridsBoundary) {
        // 记录开始时间
//        long startTime = System.nanoTime();
        // 1. 计算所有 Scene 的 boundingBox 并集
        List<Geometry> geometries = new ArrayList<>();
        for (SceneDesVO scene : scenesInfo) {
            Geometry geom = scene.getBoundingBox();
            if (geom != null && !geom.isEmpty()) {
                geometries.add(geom);
            }
        }

        Geometry unionBoundingBox = geometries.isEmpty() ? null : CascadedPolygonUnion.union(geometries);
//        // 记录结束时间并打印耗时
//        long endTime = System.nanoTime();
//        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
//        System.out.println("联合计算运行时间: " + durationMs + " ms");

        // 2. 计算覆盖度
        return calculateCoveragePercentage(unionBoundingBox, gridsBoundary).getKey();
    }
    // 计算覆盖度函数
    public AbstractMap.SimpleEntry<Double, Geometry> calculateCoveragePercentage(Geometry boundingBox, Geometry gridsBoundary) {
        // 记录开始时间
//        long startTime = System.nanoTime();
        if (boundingBox == null || boundingBox.isEmpty() ||
                gridsBoundary == null || gridsBoundary.isEmpty()) {
            return new AbstractMap.SimpleEntry<>(0.0, null);
        }

        Geometry intersection = boundingBox.intersection(gridsBoundary);
        double coverageArea = intersection.getArea();
        double gridsArea = gridsBoundary.getArea();
        double coveragePercentage = coverageArea / gridsArea;
        // 记录结束时间并打印耗时
//        long endTime = System.nanoTime();
//        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
//        System.out.println("相交计算运行时间: " + durationMs + " ms");
        return new AbstractMap.SimpleEntry<>(coveragePercentage, intersection);
    }

    /**
     * 判断场景是否属于指定分辨率分类
     */
    public boolean isSceneMatchResolutionType(SceneDesVO scene, SceneTypeByResolution type) {
        double sceneResolution = parseResolutionToMeters(scene.getResolution());
        switch (type) {
            case subMeter:
                return sceneResolution <= 1; // 亚米分辨率（≤1）
            case twoMeter:
                return sceneResolution > 1 && sceneResolution <= 2.0;
            case tenMeter:
                return sceneResolution > 2.0 && sceneResolution <= 10.0;
            case thirtyMeter:
                return sceneResolution > 10.0 && sceneResolution <= 30.0;
            case other:
                return sceneResolution > 30.0; // 其他更高分辨率
            default:
                return false;
        }
    }

    public boolean isSceneMatchResolutionType(SceneDesVO scene, String type) {
        double sceneResolution = parseResolutionToMeters(scene.getResolution());
        switch (type) {
            case "subMeter":
                return sceneResolution <= 1; // 亚米分辨率（≤1）
            case "twoMeter":
                return sceneResolution > 1 && sceneResolution <= 2.0;
            case "tenMeter":
                return sceneResolution > 2.0 && sceneResolution <= 10.0;
            case "thirtyMeter":
                return sceneResolution > 10.0 && sceneResolution <= 30.0;
            case "other":
                return sceneResolution > 30.0; // 其他更高分辨率
            default:
                return false;
        }
    }

    public double parseResolutionToMeters(String resolution) {
        if (resolution == null || !resolution.endsWith("m")) {
            throw new IllegalArgumentException("Invalid resolution format: " + resolution);
        }
        try {
            // 去掉末尾的 "m" 并解析为 double
            return Double.parseDouble(resolution.substring(0, resolution.length() - 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid resolution value: " + resolution, e);
        }
    }

    public List<SceneDesVO> queryAndCleanScenes(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Geometry gridsBoundary,
            List<String> dataType
    ) {
        long startCalTime = System.currentTimeMillis();

        List<SceneDesVO> allScenesInfo =
                getScenesByTimeAndRegion(startTime, endTime, gridsBoundary, dataType);

        long duration = System.currentTimeMillis() - startCalTime;
        log.info("从数据库中检索景消耗时间: {} ms", duration);

        int before = allScenesInfo.size();

        List<SceneDesVO> cleanedScenes = allScenesInfo.stream()
                .filter(scene -> scene != null && scene.getBoundingBox() != null)
                .collect(Collectors.toList());

        int after = cleanedScenes.size();

        if (before != after) {
            log.warn("Scene 数据清洗：{} -> {}，丢弃 {} 条（bounding_box 解析失败）",
                    before, after, before - after);
        }

        return cleanedScenes;
    }


}
