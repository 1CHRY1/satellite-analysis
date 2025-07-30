package nnu.mnr.satellite.controller.resources;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV3;
import nnu.mnr.satellite.model.dto.resources.ScenesLocationFetchDTOV3;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportWithCacheKeyVO;
import nnu.mnr.satellite.service.resources.ThemeDataServiceV3;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/v3/data/theme")
@Slf4j
public class ThemeControllerV3 {

    private final ThemeDataServiceV3 ThemeDataService;

    public ThemeControllerV3(ThemeDataServiceV3 themeDataService) {
        ThemeDataService = themeDataService;
    }

    @PostMapping("/time/region")
    public ResponseEntity<CoverageReportVO<String>> getThemesCoverageReportByTimeAndRegion(@RequestBody ScenesFetchDTOV3 scenesFetchDTO,
                                                                                   @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        CoverageReportWithCacheKeyVO<String> result = ThemeDataService.getThemesCoverageReportByTimeAndRegion(scenesFetchDTO, userId);
        return ResponseEntity.ok(result.getReport());
    }

    @PostMapping("/time/location")
    public ResponseEntity<CoverageReportVO<String>> getThemesCoverageReportByTimeAndLocation(@RequestBody ScenesLocationFetchDTOV3 scenesLocationFetchDTO,
                                                                                           @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        CoverageReportWithCacheKeyVO<String> result = ThemeDataService.getThemesCoverageReportByTimeAndLocation(scenesLocationFetchDTO, userId);
        return ResponseEntity.ok(result.getReport());
    }

}
