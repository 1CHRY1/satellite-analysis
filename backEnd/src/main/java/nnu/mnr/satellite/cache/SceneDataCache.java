package nnu.mnr.satellite.cache;

import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree; // 空间索引（R-Tree）

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SceneDataCache {
    // 用户级缓存：Key = userId + requestBody, Value = 缓存数据
    private static final ConcurrentHashMap<String, UserSceneCache> userSceneCacheMap = new ConcurrentHashMap<>();

    // 用户缓存结构
    public static class UserSceneCache {
        public List<SceneDesVO> scenesInfo;       // 原始数据
        public CoverageReportVO coverageReportVO;
        private STRtree spatialIndex;              // 空间索引（R-Tree）
        private final long cacheTime;                    // 缓存时间（用于超时清理）

        public UserSceneCache(List<SceneDesVO> scenesInfo, CoverageReportVO coverageReportVO) {
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
    }

    // 获取当前用户的缓存
    public static UserSceneCache getUserSceneCacheMap(String cacheKey) {
        return userSceneCacheMap.get(cacheKey);
    }

    // 缓存数据
    public static void cacheUserScenes(String cacheKey, List<SceneDesVO> scenesInfo, CoverageReportVO coverageReportVO) {
        userSceneCacheMap.put(cacheKey, new UserSceneCache(scenesInfo, coverageReportVO));
    }

    // 清理超时缓存（可定时任务调用）
    public static void cleanupExpiredCache(long expireTimeMs) {
        userSceneCacheMap.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue().cacheTime > expireTimeMs
        );
    }
    // 提供两个方法，以供后续调试
    // 获取当前缓存大小
    public static int getCacheSize() {
        System.out.println(userSceneCacheMap.size());
        return userSceneCacheMap.size();
    }
    // 打印所有缓存内容
    public static void printAllCacheContents() {
        System.out.println("===== Current Cache Contents =====");
        userSceneCacheMap.forEach((key, value) ->
                System.out.printf("Key: %s, Value: %s%n", key, value)
        );
        System.out.println("===================================");
    }
}
