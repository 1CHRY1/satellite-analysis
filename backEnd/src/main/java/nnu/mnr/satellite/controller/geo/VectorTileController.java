package nnu.mnr.satellite.controller.geo;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.service.geo.VectorTileService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/27 13:12
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/geo/vector/tiles")
@Slf4j
public class VectorTileController {

    private final VectorTileService vectorTileService;

    public VectorTileController(VectorTileService vectorTileService) {
        this.vectorTileService = vectorTileService;
    }

    @GetMapping("/{layerName}/{z}/{x}/{y}")
    @CrossOrigin
    public void getGeoVectorTiles(@PathVariable String layerName, @PathVariable int z, @PathVariable int x, @PathVariable int y, HttpServletResponse response) {
        byte[] tile = vectorTileService.getGeoVecterTiles(layerName, z, x, y);
        sendVectorTileResponse(tile, response);
    }

    @GetMapping("/{layerName}/region/{regionId}/type/{type}/{z}/{x}/{y}")
    public void getPatchGeoVecterTilesByParam(@PathVariable String layerName, @PathVariable String type, @PathVariable Integer regionId, @PathVariable int z, @PathVariable int x, @PathVariable int y, HttpServletResponse response) {
        byte[] tile = vectorTileService.getPatchGeoVecterTilesByParam(layerName, z, x, y, type, regionId);
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
