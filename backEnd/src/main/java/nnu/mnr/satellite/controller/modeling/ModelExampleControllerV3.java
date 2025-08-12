package nnu.mnr.satellite.controller.modeling;

import io.jsonwebtoken.JwtException;
import nnu.mnr.satellite.model.dto.modeling.VisualizationLowLevelTile;
import nnu.mnr.satellite.model.dto.modeling.VisualizationTileDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.modeling.ModelExampleServiceV3;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// 没有v2，直接v3
@RestController
@RequestMapping("/api/v3/modeling/example")
public class ModelExampleControllerV3 {

    private final ModelExampleServiceV3 modelExampleService;

    public ModelExampleControllerV3(ModelExampleServiceV3 modelExampleService) {
        this.modelExampleService = modelExampleService;
    }

    @PostMapping("/scenes/visualization")
    public ResponseEntity<CommonResultVO> createScenesVisualizationConfig(@RequestBody VisualizationTileDTO visualizationTileDTO,
                                                              @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                              @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) {
        // 拼凑cacheKey
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String cacheKey = userId + "_" + encryptedRequestBody;
        CommonResultVO result = modelExampleService.createScenesVisualizationConfig(visualizationTileDTO, cacheKey);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/scenes/visualization/lowLevel")
    public ResponseEntity<CommonResultVO> createLowLevelScenesVisualizationConfig(@RequestBody VisualizationLowLevelTile visualizationLowLevelTile,
                                                                          @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                          @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) throws IOException {
        Map<String, String> headers = new HashMap<>();
        if (authorizationHeader != null) {
            headers.put("Authorization", authorizationHeader); // 添加 Authorization 请求头
        }
        Map<String, String> cookies = new HashMap<>();
        if (encryptedRequestBody != null) {
            cookies.put("encrypted_request_body", encryptedRequestBody); // 添加 Cookie
        }

        CommonResultVO result = modelExampleService.createLowLevelScenesVisualizationConfig(visualizationLowLevelTile, headers, cookies);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/theme/visualization/{themeName}")
    public ResponseEntity<CommonResultVO> createThemeVisualizationConfig(@PathVariable String themeName,
                                                                    @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                    @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) {
        // 拼凑cacheKey
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String cacheKey = userId + "_" + encryptedRequestBody;
        CommonResultVO result = modelExampleService.createThemeVisualizationConfig(themeName, cacheKey);

        return ResponseEntity.ok(result);
    }
}
