package nnu.mnr.satellite.cache;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class EOCubeCache {
    private static final ConcurrentHashMap<String, EOCubeCache> EOCubeCacheMap = new ConcurrentHashMap<>();

    private final String cubeId;
    private final List<String> dimensionSensors;
    private final List<String> dimensionDates;
    private final List<String> dimensionBands;
    private final List<Scene> dimensionScenes;
    private final long cacheTime;

    // 构造函数
    public EOCubeCache(String cubeId, List<String> dimensionSensors, List<String> dimensionDates,
                       List<String> dimensionBands, List<Scene> dimensionScenes) {
        this.cubeId = cubeId;
        this.dimensionSensors = dimensionSensors;
        this.dimensionDates = dimensionDates;
        this.dimensionBands = dimensionBands;
        this.dimensionScenes = dimensionScenes;
        this.cacheTime = System.currentTimeMillis();
    }

    // 获取单个缓存
    public static EOCubeCache getCache(String cacheKey) {
        return EOCubeCacheMap.get(cacheKey);
    }
    // 缓存
    public static void cacheEOCube(String cacheKey, String cubeId, List<String> dimensionSensors, List<String> dimensionDates,
                                   List<String> dimensionBands, List<Scene> dimensionScenes) {
        EOCubeCacheMap.put(cacheKey, new EOCubeCache(cubeId, dimensionSensors, dimensionDates, dimensionBands, dimensionScenes));
    }

    // 获取所有缓存
    public static Map<String, EOCubeCache> getAllCaches() {
        return new ConcurrentHashMap<>(EOCubeCacheMap); // 返回副本，避免外部修改
    }

    // 获取指定用户的所有缓存
    public static Map<String, EOCubeCache> getUserCaches(String userId) {
        Map<String, EOCubeCache> result = new ConcurrentHashMap<>();
        // 假设原始缓存存储在 EOCubeCacheMap 中
        for (Map.Entry<String, EOCubeCache> entry : EOCubeCacheMap.entrySet()) {
            String cacheKey = entry.getKey();
            // 假设你的key格式是 userId + 分隔符 + 随机字符串，例如 "123_abc456"
            if (cacheKey.startsWith(userId + "_")) { // 根据你的实际key格式调整
                result.put(cacheKey, entry.getValue());
            }
        }
        return result;
    }

    @Data
    public static class Scene {
        private String sceneId;
        private String sceneTime;
        private int noData;
        private String sensorName;
        private String platformName;
        private String productName;
        private List<Image> images;
        private Map<String, String> bandMapper;
    }

    @Data
    public static class Image {
        private String bucket;
        private String tifPath;
        private String band;
    }

    // 清理超时缓存（可定时任务调用）
    public static void cleanupExpiredCache(long expireTimeMs) {
        EOCubeCacheMap.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue().cacheTime > expireTimeMs
        );
    }
}
