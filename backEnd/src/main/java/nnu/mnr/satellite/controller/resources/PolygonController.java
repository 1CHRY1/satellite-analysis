package nnu.mnr.satellite.controller.resources;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.cache.CachePolygonDTO;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV3;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportWithCacheKeyVO;
import nnu.mnr.satellite.service.resources.PolygonDataService;
import nnu.mnr.satellite.service.resources.RegionDataService;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("api/v1/data/polygon")
@Slf4j
public class PolygonController {

    private final PolygonDataService polygonDataService;

    public PolygonController(PolygonDataService polygonDataService) {
        this.polygonDataService = polygonDataService;
    }

    @PostMapping("/cache")
    public ResponseEntity<CommonResultVO> cachePolygon(@RequestBody CachePolygonDTO cachePolygonDTO,
                                                                                 @RequestHeader(value = "Authorization", required = false) String authorizationHeader) throws Exception {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok(polygonDataService.cachePolygon(userId, cachePolygonDTO));
    }
}
