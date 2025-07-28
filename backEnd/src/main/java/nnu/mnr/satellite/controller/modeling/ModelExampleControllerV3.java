package nnu.mnr.satellite.controller.modeling;

import io.jsonwebtoken.JwtException;
import nnu.mnr.satellite.model.dto.modeling.VisualizationTileDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.modeling.ModelExampleServiceV3;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 没有v2，直接v3
@RestController
@RequestMapping("/api/v3/modeling/example")
public class ModelExampleControllerV3 {

    private final ModelExampleServiceV3 modelExampleService;

    public ModelExampleControllerV3(ModelExampleServiceV3 modelExampleService) {
        this.modelExampleService = modelExampleService;
    }

    @PostMapping("/scenes/visualization")
    public ResponseEntity<CommonResultVO> createVisualizationConfig(@RequestBody VisualizationTileDTO visualizationTileDTO,
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
        CommonResultVO result = modelExampleService.createVisualizationConfig(visualizationTileDTO, cacheKey);

        return ResponseEntity.ok(result);
    }
}
