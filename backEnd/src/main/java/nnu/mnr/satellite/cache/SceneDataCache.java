package nnu.mnr.satellite.cache;

import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree; // 空间索引（R-Tree）

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SceneDataCache {
    // 用户级缓存：Key = userId + requestBody, Value = 缓存数据
    private static final ConcurrentHashMap<String, UserSceneCache> userSceneCacheMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, UserThemeCache> userThemeCacheMap = new ConcurrentHashMap<>();

    // ===============================================缓存景============================================================

    // 用户景缓存结构   后续不采用空间索引的情况下，求相交耗时0ms，似乎没必要建立空间索引
    public static class UserSceneCache {
        public List<SceneDesVO> scenesInfo;       // 原始数据
        public CoverageReportVO<Map<String, Object>> coverageReportVO;
        private STRtree spatialIndex;              // 空间索引（R-Tree）
        private final long cacheTime;                    // 缓存时间（用于超时清理）

        public UserSceneCache(List<SceneDesVO> scenesInfo, CoverageReportVO<Map<String, Object>> coverageReportVO) {
            this.scenesInfo = scenesInfo;
            this.coverageReportVO = coverageReportVO;
            this.spatialIndex = buildSpatialIndex(scenesInfo);
            this.cacheTime = System.currentTimeMillis();
        }

        // 构建空间索引（R-Tree）
        private STRtree buildSpatialIndex(List<SceneDesVO> scenesInfo) {
            STRtree index = new STRtree();
            for (SceneDesVO scene : scenesInfo) {
                Geometry geom = scene.getBoundingBox(); // 假设 SceneDesVO 有 Geometry 字段
                if (geom != null) {
                    index.insert(geom.getEnvelopeInternal(), scene); // 按包围盒插入索引
                }
            }
            return index;
        }
        // 调用空间索引
        public List<SceneDesVO> queryCandidateScenes(Geometry targetGeometry) {
            if (spatialIndex == null || targetGeometry == null) {
                return Collections.emptyList();
            }
            // 通过空间索引查询候选场景（基于包围盒快速过滤）
            @SuppressWarnings("unchecked")
            List<SceneDesVO> candidates = (List<SceneDesVO>) spatialIndex.query(targetGeometry.getEnvelopeInternal());
            return candidates != null ? candidates : Collections.emptyList();
        }
    }
    // 获取当前用户的景缓存
    public static UserSceneCache getUserSceneCacheMap(String cacheKey) {
        return userSceneCacheMap.get(cacheKey);
    }
    // 缓存景数据
    public static void cacheUserScenes(String cacheKey, List<SceneDesVO> scenesInfo, CoverageReportVO<Map<String, Object>> coverageReportVO) {
        userSceneCacheMap.put(cacheKey, new UserSceneCache(scenesInfo, coverageReportVO));
    }

    // ===============================================缓存专题=========================================================

    // 用户专题缓存结构
    public static class UserThemeCache {
        public List<SceneDesVO> scenesInfo;       // 原始数据
        public CoverageReportVO<String> coverageReportVO;
        private final long cacheTime;                    // 缓存时间（用于超时清理）

        public UserThemeCache(List<SceneDesVO> scenesInfo, CoverageReportVO<String> coverageReportVO) {
            this.scenesInfo = scenesInfo;
            this.coverageReportVO = coverageReportVO;
            this.cacheTime = System.currentTimeMillis();
        }
    }
    // 获取当前用户的专题缓存
    public static UserThemeCache getUserThemeCacheMap(String cacheKey) {
        return userThemeCacheMap.get(cacheKey);
    }
    // 缓存专题数据
    public static void cacheUserThemes(String cacheKey, List<SceneDesVO> scenesInfo, CoverageReportVO<String> coverageReportVO) {
        userThemeCacheMap.put(cacheKey, new UserThemeCache(scenesInfo, coverageReportVO));
    }

    // ==============================================================================================================

    // 提供两个方法，以供后续调试
    // 获取当前缓存大小
    public static void getCacheSize() {
        System.out.println(userSceneCacheMap.size());
        System.out.println(userThemeCacheMap.size());
    }
    // 打印所有缓存内容
    public static void printAllCacheContents() {
        System.out.println("===== Current Cache Contents =====");
        userSceneCacheMap.forEach((key, value) ->
                System.out.printf("Key: %s, Value: %s%n", key, value)
        );
        userThemeCacheMap.forEach((key, value) ->
                System.out.printf("Key: %s, Value: %s%n", key, value)
        );
        System.out.println("===================================");
    }

    // 清理超时缓存（可定时任务调用）
    public static void cleanupExpiredCache(long expireTimeMs) {
        userSceneCacheMap.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue().cacheTime > expireTimeMs
        );
        userThemeCacheMap.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue().cacheTime > expireTimeMs
        );
    }
}
