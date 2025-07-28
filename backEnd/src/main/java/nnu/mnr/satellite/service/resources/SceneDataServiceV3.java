package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.enums.common.SceneTypeByResolution;
import nnu.mnr.satellite.mapper.resources.ISceneRepoV3;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV3;
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
import java.util.stream.Collectors;

@Slf4j
@Service("SceneDataServiceV3")
public class SceneDataServiceV3 {

    @Autowired
    private RegionDataService regionDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private final ISceneRepoV3 sceneRepo;

    public SceneDataServiceV3(ISceneRepoV3 sceneRepo) {
        this.sceneRepo = sceneRepo;
    }

    public CoverageReportWithCacheKeyVO getScenesCoverageReportByTimeAndRegion(ScenesFetchDTOV3 scenesFetchDTO, String userId){
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
        List<SceneDesVO> scenesInfo;
        CoverageReportVO report = new CoverageReportVO();
        if (userSceneCache == null) {
            // 缓存未命中，从数据库中读数据
            String startTime = scenesFetchDTO.getStartTime();
            String endTime = scenesFetchDTO.getEndTime();
            Integer regionId = scenesFetchDTO.getRegionId();
            Integer resolution = scenesFetchDTO.getResolution();
            Geometry boundary = regionDataService.getRegionById(regionId).getBoundary();
            List<Integer[]> tileIds = TileCalculateUtil.getRowColByRegionAndResolution(boundary, resolution);
            Geometry gridsBoundary = GeometryUtil.getGridsBoundaryByTilesAndResolution(tileIds, resolution);
            String wkt = gridsBoundary.toText();
            String dataType = "'satellite'";
            scenesInfo = sceneRepo.getScenesInfoByTimeAndRegion(startTime, endTime, wkt, dataType);
            Integer total = scenesInfo.size();
            // 计算覆盖度
            double coverageRatio = calculateCoverageRatio(scenesInfo, gridsBoundary) * 100;
            String coverage = String.format("%.2f%%", coverageRatio);
            List<String> category = SceneTypeByResolution.getCategoryNames();
            // 4. 构建返回结果
            report.setTotal(total); // 总数据量
            report.setCoverage(coverage); // 总体覆盖率
            report.setCategory(category); // 分类名称列表

            // 5. 按分辨率分类统计
            Map<String, CoverageReportVO.DatasetItemVO> dataset = new LinkedHashMap<>();
            for (SceneTypeByResolution type : SceneTypeByResolution.values()) {
                // 初始化每个分类的 DatasetItemVO
                CoverageReportVO.DatasetItemVO item = new CoverageReportVO.DatasetItemVO();
                item.setLabel(type.getLabel());
                item.setResolution((float) type.getResolution());

                // 筛选当前分类的场景数据（根据 resolution 字段匹配）
                List<SceneDesVO> filteredScenes = scenesInfo.stream()
                        .filter(scene -> isSceneMatchResolutionType(scene, type))
                        .collect(Collectors.toList());

                item.setTotal(filteredScenes.size());
                coverageRatio = calculateCoverageRatio(filteredScenes, gridsBoundary) * 100;
                item.setCoverage(String.format("%.2f%%", coverageRatio));
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
            // 缓存数据
            SceneDataCache.cacheUserScenes(cacheKey, scenesInfo, report);
        }else {
            // 缓存命中，直接使用
            report = userSceneCache.coverageReportVO;
        }
        CoverageReportWithCacheKeyVO result = new CoverageReportWithCacheKeyVO();
        result.setReport(report);
        result.setEncryptedRequestBody(encryptedRequestBody); // 返回给 Controller 设置 Cookie
        return result;
    }

     // 计算并集后调用覆盖度函数
    public double calculateCoverageRatio(List<SceneDesVO> scenesInfo, Geometry gridsBoundary) {
        // 1. 计算所有 Scene 的 boundingBox 并集
        Geometry unionBoundingBox = null;
        for (SceneDesVO scene : scenesInfo) {
            Geometry sceneBoundingBox = scene.getBoundingBox();
            if (sceneBoundingBox == null || sceneBoundingBox.isEmpty()) {
                continue; // 跳过空数据
            }
            if (unionBoundingBox == null) {
                unionBoundingBox = sceneBoundingBox;
            } else {
                unionBoundingBox = unionBoundingBox.union(sceneBoundingBox);
            }
        }

        // 2. 计算覆盖度
        return calculateCoveragePercentage(unionBoundingBox, gridsBoundary);
    }
    // 计算覆盖度函数
    public double calculateCoveragePercentage(Geometry boundingBox, Geometry gridsBoundary) {
        if (boundingBox == null || boundingBox.isEmpty() ||
                gridsBoundary == null || gridsBoundary.isEmpty()) {
            return 0.0;
        }

        Geometry intersection = boundingBox.intersection(gridsBoundary);
        double coverageArea = intersection.getArea();
        double gridsArea = gridsBoundary.getArea();

        return coverageArea / gridsArea; // 保留两位小数
    }

    /**
     * 判断场景是否属于指定分辨率分类
     */
    private boolean isSceneMatchResolutionType(SceneDesVO scene, SceneTypeByResolution type) {
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

    public static double parseResolutionToMeters(String resolution) {
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


}
