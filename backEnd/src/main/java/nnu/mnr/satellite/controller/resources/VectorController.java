package nnu.mnr.satellite.controller.resources;

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

    @GetMapping("/{tableName}/type")
    public List<VectorTypeVO> getVectorByLocationAndTableName(@PathVariable String tableName) {
        return vectorDataService.getVectorTypeByTableName(tableName);
    }

    // 矢量数据这里传递的参数要改
    @GetMapping("/region/{regionId}/{tableName}/{z}/{x}/{y}")
    public void getVectorByRegionAndTableName(@PathVariable Integer regionId, @PathVariable String tableName,
                                              @PathVariable int z, @PathVariable int x,
                                              @PathVariable int y, @RequestParam(value = "type", required = false) Integer type,
                                              HttpServletResponse response,
                                              @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                              @CookieValue(value = "encrypted_request_body", required = false) String encryptedRequestBody) {
        // 拼凑cacheKey
        String userId;
        userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);

        String cacheKey = userId + "_" + encryptedRequestBody;
        byte[] tile = vectorDataService.getVectorByRegionAndTableName(tableName, z, x, y, cacheKey, type);
        sendVectorTileResponse(tile, response);
    }

    @GetMapping("/grid/{columnId}/{rowId}/{resolution}/{tableName}/{z}/{x}/{y}")
    public void getVectorByGridResolutionAndTableName(@PathVariable Integer columnId,
                                              @PathVariable Integer rowId,
                                              @PathVariable Integer resolution,
                                              @PathVariable String tableName,
                                              @PathVariable int z, @PathVariable int x,
                                              @PathVariable int y, @RequestParam(value = "type", required = false) Integer type,
                                                      HttpServletResponse response) {
        byte[] tile = vectorDataService.getVectorByGridResolutionAndTableName(columnId, rowId, resolution, tableName, z, x, y, type);
        sendVectorTileResponse(tile, response);
    }

    @GetMapping("/location/{locationId}/{resolution}/{tableName}/{z}/{x}/{y}")
    public void getVectorByLocationAndTableName(@PathVariable String locationId,
                                              @PathVariable Integer resolution,
                                              @PathVariable String tableName,
                                              @PathVariable int z, @PathVariable int x,
                                              @PathVariable int y, @RequestParam(value = "type", required = false) Integer type,
                                                HttpServletResponse response) {
        byte[] tile = vectorDataService.getVectorByLocationAndTableName(locationId, resolution, tableName, z, x, y, type);
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
}
