package nnu.mnr.satellite.controller.resources;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.GridBasicDTO;
import nnu.mnr.satellite.model.dto.resources.GridsWithFiltersDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.GridBoundaryVO;
import nnu.mnr.satellite.model.vo.resources.GridsScenesOverlapVO;
import nnu.mnr.satellite.service.resources.GridDataServiceV3;
import nnu.mnr.satellite.utils.common.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/v3/data/grid")
@Slf4j
public class GridControllerV3 {

    @Autowired
    private GridDataServiceV3 gridDataService;

    @PostMapping("/scene")
    public ResponseEntity<CoverageReportVO<JSONObject>> getScenesByGridAndResolution(@RequestBody GridBasicDTO gridBasicDTO,
                                                                                     @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                                     @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) throws IOException {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        String cacheKey = userId + "_" + encryptedRequestBody;
        return ResponseEntity.ok(gridDataService.getScenesByGridAndResolution(gridBasicDTO, cacheKey));
    }

    @PostMapping("/theme")
    public ResponseEntity<CoverageReportVO<JSONObject>> getThemesByGridAndResolution(@RequestBody GridBasicDTO gridBasicDTO,
                                                                                     @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                                     @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) throws IOException {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        String cacheKey = userId + "_" + encryptedRequestBody;
        return ResponseEntity.ok(gridDataService.getThemesByGridAndResolution(gridBasicDTO, cacheKey));
    }

    @PostMapping("/scene/contain")
    public ResponseEntity<GridsScenesOverlapVO> getScenesByGridsAndFilters(@RequestBody GridsWithFiltersDTO gridsWithFiltersDTO,
                                                                           @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                           @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) throws IOException {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        String cacheKey = userId + "_" + encryptedRequestBody;
        return ResponseEntity.ok(gridDataService.getScenesByGridsAndFilters(gridsWithFiltersDTO, cacheKey));
    }

    @PostMapping("/resolution/columnIdAndRowId")
    public ResponseEntity<GridBoundaryVO> getBoundaryByResolutionAndId(@RequestBody GridBasicDTO gridBasicDTO) throws IOException {
        return ResponseEntity.ok(gridDataService.getBoundaryByResolutionAndId(gridBasicDTO));
    }



}
