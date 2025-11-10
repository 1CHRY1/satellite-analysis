package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.cache.DeleteCacheDTO;
import nnu.mnr.satellite.model.dto.admin.cache.DeleteRedisCacheDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/cache")
public class AdminCacheController {

    @Autowired
    private AdminCacheService adminCacheService;

    @GetMapping("/backend/get")
    public ResponseEntity<CommonResultVO> getCache()  {
        return ResponseEntity.ok(adminCacheService.getCache());
    }

    @GetMapping("/redis/get")
    public ResponseEntity<CommonResultVO> getRedisCache()  {
        return ResponseEntity.ok(adminCacheService.getRedisCache());
    }

    @DeleteMapping("/backend/delete")
    public ResponseEntity<CommonResultVO> deleteCache(@RequestBody DeleteCacheDTO deleteCacheDTO)  {
        return ResponseEntity.ok(adminCacheService.deleteCache(deleteCacheDTO));
    }

    @DeleteMapping("/backend/delete/expire")
    public ResponseEntity<CommonResultVO> deleteCache()  {
        return ResponseEntity.ok(adminCacheService.deleteExpireCache());
    }

    @DeleteMapping("/redis/delete")
    public ResponseEntity<CommonResultVO> deleteRedisCache(@RequestBody DeleteRedisCacheDTO deleteRedisCacheDTO)  {
        return ResponseEntity.ok(adminCacheService.deleteRedisCache(deleteRedisCacheDTO));
    }

}
