package nnu.mnr.satellite.cache;

import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class PolygonCache {
    private static final ConcurrentHashMap<String, PolygonCache> polygonCacheMap = new ConcurrentHashMap<>();

    private final String polygonId;
    private final Geometry geometry; // 手绘多边形边界
    private Geometry gridsBoundary;
    private final long cacheTime;

    // 构造函数
    public PolygonCache(String polygonId, Geometry geometry) {
        this.polygonId = polygonId;
        this.geometry = geometry;
        this.cacheTime = System.currentTimeMillis();
    }

    // 构造函数
    public PolygonCache(String polygonId, Geometry geometry, Geometry gridsBoundary) {
        this.polygonId = polygonId;
        this.geometry = geometry;
        this.gridsBoundary = gridsBoundary;
        this.cacheTime = System.currentTimeMillis();
    }

    // 获取单个缓存
    public static PolygonCache getCache(String cacheKey) {
        return polygonCacheMap.get(cacheKey);
    }

    // 缓存
    public static void cachePolygon(String cacheKey, String polygonId, Geometry geometry) {
        polygonCacheMap.put(cacheKey, new PolygonCache(polygonId, geometry));
    }
    public static void updateGridsBoundary(String cacheKey, Geometry gridsBoundary) {
        PolygonCache cache = polygonCacheMap.get(cacheKey);
        if (cache != null) {
            cache.setGridsBoundary(gridsBoundary);
        }
    }

    // 获取所有缓存
    public static Map<String, PolygonCache> getAllCaches() {
        return new ConcurrentHashMap<>(polygonCacheMap); // 返回副本，避免外部修改
    }
}
