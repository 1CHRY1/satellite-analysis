package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.cache.CachePageDTO;
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

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getCachePage(@RequestBody CachePageDTO cachePageDTO) {
        return ResponseEntity.ok(adminCacheService.getCachePage(cachePageDTO));
    }

}
