package nnu.mnr.satellite.controller.resources;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV3;
import nnu.mnr.satellite.model.dto.resources.ScenesLocationFetchDTOV3;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportWithCacheKeyVO;
import nnu.mnr.satellite.service.resources.SceneDataServiceV3;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("api/v3/data/scene")
@Slf4j
public class SceneControllerV3 {

    private final SceneDataServiceV3 sceneDataService;

    public SceneControllerV3(SceneDataServiceV3 sceneDataService) {
        this.sceneDataService = sceneDataService;
    }

    @PostMapping("/time/region")
    public ResponseEntity<CoverageReportVO<Map<String, Object>>> getScenesCoverageReportByTimeAndRegion(@RequestBody ScenesFetchDTOV3 scenesFetchDTO,
                                                                                        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                                        HttpServletResponse response) {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        CoverageReportWithCacheKeyVO<Map<String, Object>> result = sceneDataService.getScenesCoverageReportByTimeAndRegion(scenesFetchDTO, userId);
        // 设置cookie
        Cookie cookie = new Cookie("encrypted_request_body", result.getEncryptedRequestBody());
        cookie.setPath("/"); // 设置 Cookie 作用路径
        cookie.setHttpOnly(true); // 防止 XSS 攻击
        cookie.setMaxAge(-1); // 默认，浏览器关闭后自动删除
        response.addCookie(cookie);
        return ResponseEntity.ok(result.getReport());
    }

    @PostMapping("/time/location")
    public ResponseEntity<CoverageReportVO<Map<String, Object>>> getScenesCoverageReportByTimeAndLocation(@RequestBody ScenesLocationFetchDTOV3 scenesLocationFetchDTOV3,
                                                                                                        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                                                        HttpServletResponse response) {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        CoverageReportWithCacheKeyVO<Map<String, Object>> result = sceneDataService.getScenesCoverageReportByTimeAndLocation(scenesLocationFetchDTOV3, userId);
        // 设置cookie
        Cookie cookie = new Cookie("encrypted_request_body", result.getEncryptedRequestBody());
        cookie.setPath("/"); // 设置 Cookie 作用路径
        cookie.setHttpOnly(true); // 防止 XSS 攻击
        cookie.setMaxAge(-1); // 默认，浏览器关闭后自动删除
        response.addCookie(cookie);
        return ResponseEntity.ok(result.getReport());
    }

    @GetMapping("/coverage")
    public  ResponseEntity<CommonResultVO> getCoverageByCategory(@RequestParam(value = "category", required = false) String category,
                                                                 @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                 @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody){
        // 拼凑cacheKey
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String cacheKey = userId + "_" + encryptedRequestBody;
        return ResponseEntity.ok(sceneDataService.getCoverageByCategory(category, cacheKey));
    }



}
