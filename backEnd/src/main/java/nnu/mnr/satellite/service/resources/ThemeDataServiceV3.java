package nnu.mnr.satellite.service.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.enums.common.SceneTypeByTheme;
import nnu.mnr.satellite.mapper.resources.ISceneRepoV3;
import nnu.mnr.satellite.model.dto.cache.CacheDataDTO;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV3;
import nnu.mnr.satellite.model.dto.resources.ScenesLocationFetchDTOV3;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportWithCacheKeyVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service("ThemeDataServiceV3")
public class ThemeDataServiceV3 {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegionDataService regionDataService;

    @Autowired
    private ISceneRepoV3 sceneRepo;

    @Autowired
    private LocationService locationService;

    @Autowired
    private SceneDataServiceV3 sceneDataService;

    public CoverageReportWithCacheKeyVO<String> getThemesCoverageReportByTimeAndRegion(ScenesFetchDTOV3 scenesFetchDTO, String userId){
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
        String startTime = scenesFetchDTO.getStartTime();
        String endTime = scenesFetchDTO.getEndTime();
        Integer regionId = scenesFetchDTO.getRegionId();
        Integer resolution = scenesFetchDTO.getResolution();
        CoverageReportVO<String> report;
        if (userThemeCache == null && userSceneCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry boundary = regionDataService.getRegionById(regionId).getBoundary();
            List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
            Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
            String themeCodes = SceneTypeByTheme.getAllCodes().stream()
                    .map(code -> "'" + code + "'")
                    .collect(Collectors.joining(", "));
            String dataType = "'satellite', " + themeCodes;
            List<SceneDesVO> allScenesInfo = sceneDataService.getScenesByTimeAndRegion(startTime, endTime, gridsBoundary, dataType);
            allScenesInfo.sort((s1, s2) -> s2.getSceneTime().compareTo(s1.getSceneTime()));
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

            report = buildCoverageReport(themesInfo);

            // 缓存数据
            SceneDataCache.cacheUserThemes(cacheKey, themesInfo, report);
            SceneDataCache.cacheUserScenes(cacheKey, scenesInfo, null);
        } else if (userThemeCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry boundary = regionDataService.getRegionById(regionId).getBoundary();
            List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
            Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
            String dataType = SceneTypeByTheme.getAllCodes().stream()
                    .map(code -> "'" + code + "'")
                    .collect(Collectors.joining(", "));

            List<SceneDesVO> themesInfo = sceneDataService.getScenesByTimeAndRegion(startTime, endTime, gridsBoundary, dataType);
            themesInfo.sort((s1, s2) -> s2.getSceneTime().compareTo(s1.getSceneTime()));

            report = buildCoverageReport(themesInfo);

            // 缓存数据
            SceneDataCache.cacheUserThemes(cacheKey, themesInfo, report);
        } else if (userThemeCache.coverageReportVO == null) {
            report = buildCoverageReport(userThemeCache.scenesInfo);
            // 缓存数据
            SceneDataCache.cacheUserThemes(cacheKey, userThemeCache.scenesInfo, report);
        } else {
            // 缓存命中，直接使用
            report = userThemeCache.coverageReportVO;
        }
        CoverageReportWithCacheKeyVO<String> result = new CoverageReportWithCacheKeyVO<>();
        result.setReport(report);
        result.setEncryptedRequestBody(encryptedRequestBody); // 返回给 Controller 设置 Cookie
        return result;
    }

    public CoverageReportWithCacheKeyVO<String> getThemesCoverageReportByTimeAndLocation(ScenesLocationFetchDTOV3 scenesLocationFetchDTO, String userId){
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
        String startTime = scenesLocationFetchDTO.getStartTime();
        String endTime = scenesLocationFetchDTO.getEndTime();
        String locationId = scenesLocationFetchDTO.getLocationId();
        Integer resolution = scenesLocationFetchDTO.getResolution();
        CoverageReportVO<String> report;
        if (userThemeCache == null && userSceneCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry gridsBoundary = locationService.getLocationBoundary(resolution, locationId);
            String themeCodes = SceneTypeByTheme.getAllCodes().stream()
                    .map(code -> "'" + code + "'")
                    .collect(Collectors.joining(", "));
            String dataType = "'satellite', " + themeCodes;
            List<SceneDesVO> allScenesInfo = sceneDataService.getScenesByTimeAndRegion(startTime, endTime, gridsBoundary, dataType);
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

            report = buildCoverageReport(themesInfo);

            // 缓存数据
            SceneDataCache.cacheUserThemes(cacheKey, themesInfo, report);
            SceneDataCache.cacheUserScenes(cacheKey, scenesInfo, null);
        } else if (userThemeCache == null) {
            // 缓存未命中，从数据库中读数据
            Geometry gridsBoundary = locationService.getLocationBoundary(resolution, locationId);
            String dataType = SceneTypeByTheme.getAllCodes().stream()
                    .map(code -> "'" + code + "'")
                    .collect(Collectors.joining(", "));

            List<SceneDesVO> themesInfo = sceneDataService.getScenesByTimeAndRegion(startTime, endTime, gridsBoundary, dataType);

            report = buildCoverageReport(themesInfo);

            // 缓存数据
            SceneDataCache.cacheUserThemes(cacheKey, themesInfo, report);
        } else if (userThemeCache.coverageReportVO == null) {
            report = buildCoverageReport(userThemeCache.scenesInfo);
            // 缓存数据
            SceneDataCache.cacheUserThemes(cacheKey, userThemeCache.scenesInfo, report);
        } else {
            // 缓存命中，直接使用
            report = userThemeCache.coverageReportVO;
        }
        CoverageReportWithCacheKeyVO<String> result = new CoverageReportWithCacheKeyVO<>();
        result.setReport(report);
        result.setEncryptedRequestBody(encryptedRequestBody); // 返回给 Controller 设置 Cookie
        return result;
    }

    private CoverageReportVO<String> buildCoverageReport(List<SceneDesVO> scenesInfo){
        CoverageReportVO<String> report = new CoverageReportVO<>();
        Integer total = scenesInfo.size();
        List<String> category = SceneTypeByTheme.getAllCodes();
        // 4. 构建返回结果
        report.setTotal(total); // 总数据量
        report.setCategory(category); // 分类名称列表

        // 5. 按专题类型分类统计
        Map<String, CoverageReportVO.DatasetItemVO<String>> dataset = new LinkedHashMap<>();

        for (SceneTypeByTheme type : SceneTypeByTheme.values()) {
            // 初始化每个分类的 DatasetItemVO
            CoverageReportVO.DatasetItemVO<String> item = new CoverageReportVO.DatasetItemVO<>();
            item.setLabel(type.getLabel());
            // 筛选当前分类的场景数据（根据 datatype 字段分类）
            List<SceneDesVO> filteredScenes = scenesInfo.stream()
                    .filter(scene -> type.getCode().equalsIgnoreCase(scene.getDataType()))
                    .toList();

            item.setTotal(filteredScenes.size());

            List<String> uniqueSceneNames = filteredScenes.stream()
                    .map(SceneDesVO::getSceneName)
                    .distinct()
                    .collect(Collectors.toList());

            item.setDataList(uniqueSceneNames);
            dataset.put(type.getCode(), item);
        }
        report.setDataset(dataset);
        return report;
    }

}
