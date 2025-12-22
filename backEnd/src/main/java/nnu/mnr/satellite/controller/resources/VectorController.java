package nnu.mnr.satellite.controller.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.VectorDataDTO;
import nnu.mnr.satellite.model.dto.resources.VectorsFetchDTO;
import nnu.mnr.satellite.model.dto.resources.VectorsLocationFetchDTO;
import nnu.mnr.satellite.model.vo.resources.VectorInfoVO;
import nnu.mnr.satellite.model.vo.resources.VectorTypeVO;
import nnu.mnr.satellite.service.resources.VectorDataService;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/data/vector")
@Slf4j
public class VectorController {

    @Autowired
    private VectorDataService vectorDataService;

    @PostMapping("/time/region")
    public ResponseEntity<List<VectorInfoVO>> getVectorByTimeAndRegion(@RequestBody VectorsFetchDTO vectorsFetchDTO) {
        return ResponseEntity.ok(vectorDataService.getVectorByTimeAndRegion(vectorsFetchDTO));
    }

    @PostMapping("/time/location")
    public ResponseEntity<List<VectorInfoVO>> getVectorByTimeAndLocation(@RequestBody VectorsLocationFetchDTO vectorsLocationFetchDTO) {
        return ResponseEntity.ok(vectorDataService.getVectorByTimeAndLocation(vectorsLocationFetchDTO));
    }

//    @GetMapping("/{tableName}/type")
//    public List<VectorTypeVO> getVectorTypeByTableName(@PathVariable String tableName) {
//        return vectorDataService.getVectorTypeByTableName(tableName);
//    }

    @GetMapping("/{tableName}/discrete/{field}")
    public List<String> getVectorTypeByTableNameAndField(@PathVariable String tableName, @PathVariable String field,
                                                         @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                         @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) {
        // 拼凑cacheKey
        String userId;
        userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        String cacheKey = userId + "_" + encryptedRequestBody;
        return vectorDataService.getVectorTypeByTableNameAndField(tableName, field, cacheKey);
    }

    @GetMapping("/{tableName}/continuous/{field}")
    public List<JSONObject> getVectorTypeByTableNameAndFieldAndCount(@PathVariable String tableName, @PathVariable String field,
                                                                     @RequestParam Integer count,
                                                                     @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                                     @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) {
        // 拼凑cacheKey
        String userId;
        userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        String cacheKey = userId + "_" + encryptedRequestBody;
        return vectorDataService.getVectorTypeByTableNameAndFieldAndCount(tableName, field, count,cacheKey);
    }

    // 矢量数据这里传递的参数要改
    @GetMapping("/region/{regionId}/{tableName}/{field}/{z}/{x}/{y}")
    public void getVectorByRegionAndTableName(@PathVariable Integer regionId, @PathVariable String tableName,
                                              @PathVariable String field,
                                              @PathVariable int z, @PathVariable int x,
                                              @PathVariable int y, @RequestParam(value = "types", required = false) List<String> types,
                                              HttpServletResponse response,
                                              @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                              @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) throws IOException {
        // 验证 field 是否合法
        if (isRestrictedField(field)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            response.getWriter().write("Invalid field: 'id' and 'geom' are restricted.");
            return;
        }
        // 拼凑cacheKey
        String userId;
        userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);

        String cacheKey = userId + "_" + encryptedRequestBody;
        byte[] tile = vectorDataService.getVectorByRegionAndTableName(tableName, field, z, x, y, cacheKey, types);
        sendVectorTileResponse(tile, response);
    }

    @GetMapping("/grid/{columnId}/{rowId}/{resolution}/{tableName}/{field}/{z}/{x}/{y}")
    public void getVectorByGridResolutionAndTableName(@PathVariable Integer columnId,
                                              @PathVariable Integer rowId,
                                              @PathVariable Integer resolution,
                                              @PathVariable String tableName, @PathVariable String field,
                                              @PathVariable int z, @PathVariable int x,
                                              @PathVariable int y, @RequestParam(value = "types", required = false) List<String> types,
                                                      HttpServletResponse response) throws IOException {
        // 验证 field 是否合法
        if (isRestrictedField(field)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            response.getWriter().write("Invalid field: 'id' and 'geom' are restricted.");
            return;
        }

        byte[] tile = vectorDataService.getVectorByGridResolutionAndTableName(columnId, rowId, resolution, tableName, field, z, x, y, types);
        sendVectorTileResponse(tile, response);
    }

    @GetMapping("/location/{locationId}/{resolution}/{tableName}/{field}/{z}/{x}/{y}")
    public void getVectorByLocationAndTableName(@PathVariable String locationId,
                                              @PathVariable Integer resolution,
                                              @PathVariable String tableName, @PathVariable String field,
                                              @PathVariable int z, @PathVariable int x,
                                              @PathVariable int y, @RequestParam(value = "types", required = false) List<String> types,
                                                HttpServletResponse response) throws IOException {
        // 验证 field 是否合法
        if (isRestrictedField(field)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            response.getWriter().write("Invalid field: 'id' and 'geom' are restricted.");
            return;
        }

        byte[] tile = vectorDataService.getVectorByLocationAndTableName(locationId, resolution, tableName, field, z, x, y, types);
        sendVectorTileResponse(tile, response);
    }

    private void sendVectorTileResponse(byte[] tileRes, HttpServletResponse response) {
        if(tileRes == null) {
            return;
        }
        ServletOutputStream sos;
        try {
            response.setContentType("application/octet-stream");
            sos = response.getOutputStream();
            sos.write(tileRes);
            sos.flush();
            sos.close();
        } catch (org.apache.catalina.connector.ClientAbortException e) {
            //地图移动时客户端主动取消， 产生异常"你的主机中的软件中止了一个已建立的连接"，无需处理
            log.info("Map moved. Client end MVT connection.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isRestrictedField(String field) {
        return field != null && (field.toLowerCase().contains("id") || "geom".equalsIgnoreCase(field));
    }
}
