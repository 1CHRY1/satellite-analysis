package nnu.mnr.satellite.service.admin;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.EOCubeCache;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.model.dto.admin.cache.DeleteCacheDTO;
import nnu.mnr.satellite.model.dto.admin.cache.DeleteRedisCacheDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.utils.dt.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static nnu.mnr.satellite.service.cache.CacheCleanerService.DEFAULT_SCENE_CACHE_EXPIRY_MS;
import static nnu.mnr.satellite.service.cache.CacheCleanerService.DEFAULT_CUBE_CACHE_EXPIRY_MS;

@Slf4j
@Service
public class AdminCacheService {

    @Autowired
    private RedisUtil redisUtil;

    public CommonResultVO getCache() {
        // 获取SceneDataCache
        Map<String, Map<String, Object>> allSceneDataCaches = SceneDataCache.getAllCaches();
        // 处理景缓存，只获取有用的部分（主要是cacheTime）
        Map<String, Object> allSceneCaches = allSceneDataCaches.get("allSceneCache");
        Map<String, Object> sceneCaches = new HashMap<>();
        allSceneCaches.forEach((cacheKey, userSceneCacheObj) -> {
            SceneDataCache.UserSceneCache userSceneCache = (SceneDataCache.UserSceneCache) userSceneCacheObj;
            // 创建一个临时 Map 存储需要的字段
            Map<String, Object> extractedData = new HashMap<>();

            extractedData.put("cacheTime", userSceneCache.getCacheTime());
            extractedData.put("cacheExpiry", userSceneCache.getCacheTime() + DEFAULT_SCENE_CACHE_EXPIRY_MS);

            // 将临时 Map 放入主 Map
            sceneCaches.put(cacheKey, extractedData);
        });
        // 处理专题缓存
        Map<String, Object> allThemeCaches = allSceneDataCaches.get("allThemeCache");
        Map<String, Object> themeCaches = new HashMap<>();
        allThemeCaches.forEach((cacheKey, userThemeCacheObj) -> {
            SceneDataCache.UserThemeCache userThemeCache = (SceneDataCache.UserThemeCache) userThemeCacheObj;
            // 创建一个临时 Map 存储需要的字段
            Map<String, Object> extractedData = new HashMap<>();

            extractedData.put("cacheTime", userThemeCache.getCacheTime());
            extractedData.put("cacheExpiry", userThemeCache.getCacheTime() + DEFAULT_SCENE_CACHE_EXPIRY_MS);

            // 将临时 Map 放入主 Map
            themeCaches.put(cacheKey, extractedData);
        });
        // 处理格网缓存
        Map<String, Object> allRegionInfoCaches = allSceneDataCaches.get("allRegionInfoCache");
        Map<String, Object> regionInfoCaches = new HashMap<>();
        allRegionInfoCaches.forEach((cacheKey, userRegionInfoCacheObj) -> {
            SceneDataCache.UserRegionInfoCache userRegionInfoCache = (SceneDataCache.UserRegionInfoCache) userRegionInfoCacheObj;
            // 创建一个临时 Map 存储需要的字段
            Map<String, Object> extractedData = new HashMap<>();

            extractedData.put("cacheTime", userRegionInfoCache.getCacheTime());
            extractedData.put("cacheExpiry", userRegionInfoCache.getCacheTime() + DEFAULT_SCENE_CACHE_EXPIRY_MS);

            // 将临时 Map 放入主 Map
            regionInfoCaches.put(cacheKey, extractedData);
        });
        // 获取时空立方体缓存
        Map<String, Object> eOCubeCaches = EOCubeCache.getAllCaches();
        Map<String, Object> processedEOCubeCaches = new HashMap<>();
        eOCubeCaches.forEach((cacheKey, eOCubeCacheObj) -> {
            EOCubeCache eOCubeCache = (EOCubeCache) eOCubeCacheObj;
            // 创建一个临时 Map 存储需要的字段
            Map<String, Object> extractedData = new HashMap<>();
            extractedData.put("cacheTime", eOCubeCache.getCacheTime());
            extractedData.put("cacheExpiry", eOCubeCache.getCacheTime() + DEFAULT_CUBE_CACHE_EXPIRY_MS);
            processedEOCubeCaches.put(cacheKey, extractedData);
        });

        Map<String, Object> mergedCache = new HashMap<>();
        mergedCache.put("eOCubeCache", processedEOCubeCaches);
        mergedCache.put("sceneCache", sceneCaches);
        mergedCache.put("themeCache", themeCaches);
        mergedCache.put("regionInfoCache", regionInfoCaches);
        return CommonResultVO.builder()
                .status(1)
                .message("获取缓存成功")
                .data(mergedCache)
                .build();
    }

    public CommonResultVO getRedisCache(){
        Set<String> keys = redisUtil.getKeys();
        if (keys == null || keys.isEmpty()) {
            return CommonResultVO.builder()
                    .status(1)
                    .message("Redis缓存为空")
                    .data(new HashSet<>())
                    .build();
        }else {
            Set<Object> allRedisCaches = new HashSet<>();
            keys.forEach(key -> {
                Map<String, Object> extractedData = new HashMap<>();
                String type = redisUtil.getKeyType(key);
                Long ttl = redisUtil.getTime(key);
                Long size = redisUtil.getStringSize(key);
                extractedData.put("key", key);
                extractedData.put("type", type);
                extractedData.put("ttl", ttl);
                extractedData.put("size", size);
                allRedisCaches.add(extractedData);
            });
            return CommonResultVO.builder()
                    .status(1)
                    .message("获取redis缓存成功")
                    .data(allRedisCaches)
                    .build();
        }
    }

    public CommonResultVO deleteCache(DeleteCacheDTO deleteCacheDTO){
        List<DeleteCacheDTO.DeleteCache> cacheKeys = deleteCacheDTO.getCacheKeys();
        cacheKeys.forEach(cacheKey -> {
            if (Set.of("sceneCache", "themeCache", "regionInfoCache").contains(cacheKey.getType())) {
                SceneDataCache.deleteCacheByKey(cacheKey.getCacheKey(), cacheKey.getType());
            }else if (Objects.equals("eOCubeCache", cacheKey.getType())) {
                EOCubeCache.deleteCaches(cacheKey.getCacheKey());
            }else {
                throw new IllegalArgumentException("Invalid cacheType. Supported types: 'sceneCache', 'themeCache', 'regionInfoCache', 'eOCubeCache'");
            }
        });
        return CommonResultVO.builder()
                .status(1)
                .message("删除缓存成功")
                .build();
    }

    public CommonResultVO deleteExpireCache(){
        SceneDataCache.cleanupExpiredCache(DEFAULT_SCENE_CACHE_EXPIRY_MS);
        EOCubeCache.cleanupExpiredCache(DEFAULT_CUBE_CACHE_EXPIRY_MS);
        System.out.println("Cache cleaned at: " + LocalDateTime.now());
        return CommonResultVO.builder()
                .status(1)
                .message("删除过期缓存成功")
                .build();
    }

    public CommonResultVO deleteRedisCache(DeleteRedisCacheDTO deleteRedisCacheDTO){
        List<String> keys = deleteRedisCacheDTO.getKeys();
        redisUtil.deleteMultipleKeys(keys);
        return CommonResultVO.builder()
                .status(1)
                .message("删除redis缓存成功")
                .build();
    }



}
