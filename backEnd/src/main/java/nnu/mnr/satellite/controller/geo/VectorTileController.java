package nnu.mnr.satellite.controller.geo;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import nnu.mnr.satellite.service.geo.VectorTileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/27 13:12
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/geo")
public class VectorTileController {

    private final VectorTileService vectorTileService;

    public VectorTileController(VectorTileService vectorTileService) {
        this.vectorTileService = vectorTileService;
    }

    @GetMapping("/vector/tiles/{layerName}/{z}/{x}/{y}")
    public void getGeoVectorTiles(@PathVariable String layerName, @PathVariable int z, @PathVariable int x, @PathVariable int y, HttpServletResponse response) {
        byte[] tile = vectorTileService.getGeoVecterTiles(layerName, z, x, y);
        sendVectorTileResponse(tile, response);
    }

    @GetMapping("/vector/tiles/{layerName}/{param}/{value}/{z}/{x}/{y}")
    public void getGeoVectorTilesByParam(@PathVariable String param, @PathVariable String value, @PathVariable String layerName, @PathVariable int z, @PathVariable int x, @PathVariable int y, HttpServletResponse response) {
        byte[] tile = vectorTileService.getGeoVecterTilesByParam(param, value, layerName, z, x, y);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
