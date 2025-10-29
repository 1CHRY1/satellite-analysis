package nnu.mnr.satellite.service.admin;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.EOCubeCache;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.model.dto.admin.cache.CachePageDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AdminCacheService {

    public CommonResultVO getCachePage(CachePageDTO cachePageDTO){
        Map<String, EOCubeCache> eOCubeCache = EOCubeCache.getAllCaches();

        return CommonResultVO.builder()
                .status(1)
                .message("获取缓存成功")
                .data(null)
                .build();
    }

}
