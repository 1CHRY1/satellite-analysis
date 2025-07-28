package nnu.mnr.satellite.controller.resources;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.ScenesFetchDTOV3;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportWithCacheKeyVO;
import nnu.mnr.satellite.service.resources.SceneDataServiceV3;
import nnu.mnr.satellite.utils.common.IdUtil;
import nnu.mnr.satellite.utils.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v3/data/scene")
@Slf4j
public class SceneControllerV3 {

    private final SceneDataServiceV3 sceneDataService;

    @Autowired
    private HttpServletRequest request;

    public SceneControllerV3(SceneDataServiceV3 sceneDataService) {
        this.sceneDataService = sceneDataService;
    }

    @PostMapping("/time/region")
    public ResponseEntity<CoverageReportVO> getScenesCoverageReportByTimeAndRegion(@RequestBody ScenesFetchDTOV3 scenesFetchDTO,
                                                                                   @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                                   HttpServletResponse response) throws IOException {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        CoverageReportWithCacheKeyVO result = sceneDataService.getScenesCoverageReportByTimeAndRegion(scenesFetchDTO, userId);
        // 设置cookie
        Cookie cookie = new Cookie("encrypted_request_body", result.getEncryptedRequestBody());
        cookie.setPath("/"); // 设置 Cookie 作用路径
        cookie.setHttpOnly(true); // 防止 XSS 攻击
        cookie.setMaxAge(-1); // 默认，浏览器关闭后自动删除
        response.addCookie(cookie);
        return ResponseEntity.ok(result.getReport());
    }



}
