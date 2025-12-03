package nnu.mnr.satellite.service.cache;

import nnu.mnr.satellite.cache.EOCubeCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import nnu.mnr.satellite.cache.SceneDataCache;

import java.time.LocalDateTime;

@Service
public class CacheCleanerService {

    public static final long DEFAULT_SCENE_CACHE_EXPIRY_MS = 3600000L;
    public static final long DEFAULT_CUBE_CACHE_EXPIRY_MS = 604800000L;

    // 每天凌晨 3 点执行缓存清理（Cron 表达式）
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanCacheDaily() {
        SceneDataCache.cleanupExpiredCache(DEFAULT_SCENE_CACHE_EXPIRY_MS);
        EOCubeCache.cleanupExpiredCache(DEFAULT_CUBE_CACHE_EXPIRY_MS);
        System.out.println("Cache cleaned at: " + LocalDateTime.now());
    }

    // 或者固定间隔清理（每 6 小时一次）
//    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 小时（毫秒）
//    public void cleanCachePeriodically() {
//        SceneDataCache.cleanupExpiredCache(DEFAULT_CACHE_EXPIRY_MS);
//        EOCubeCache.cleanupExpiredCache(DEFAULT_CACHE_EXPIRY_MS);
//        System.out.println("Cache cleaned at: " + LocalDateTime.now());
//    }
}