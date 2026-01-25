package nnu.mnr.satellite.controller.modeling;

import io.jsonwebtoken.JwtException;
import nnu.mnr.satellite.model.dto.modeling.*;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.modeling.ModelExampleService;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/9 22:07
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/modeling/example")
public class ModelExampleController {

    private final ModelExampleService modelExampleService;

    public ModelExampleController(ModelExampleService modelExampleService) {
        this.modelExampleService = modelExampleService;
    }

    @PostMapping("/noCloud")
    public ResponseEntity<CommonResultVO> getNoCloudByRegion(@RequestBody NoCloudFetchDTO noCloudFetchDTO,
                                                             @RequestHeader(value = "Authorization", required = false) String authorizationHeader) throws IOException {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok(modelExampleService.getNoCloudByRegion(noCloudFetchDTO, userId));
    }

    @PostMapping("/noCloud/createNoCloudConfig")
    public ResponseEntity<CommonResultVO> createNoCloudConfig(@RequestBody NoCloudTileDTO noCloudTileDTO) {
        return ResponseEntity.ok(modelExampleService.createNoCloudConfig(noCloudTileDTO));
    }

    @PostMapping("/ndvi/point")
    public ResponseEntity<CommonResultVO> getNdviByRegion(@RequestBody NdviFetchDTO ndviFetchDTO) {
        return ResponseEntity.ok(modelExampleService.getNDVIByPoint(ndviFetchDTO));
    }

    @PostMapping("/spectrum/point")
    public ResponseEntity<CommonResultVO> getSpectrumByPoint(@RequestBody SpectrumDTO spectrumDTO) {
        return ResponseEntity.ok(modelExampleService.getSpectrumByPoint(spectrumDTO));
    }

    @PostMapping("/raster/point")
    public ResponseEntity<CommonResultVO> getRasterResultByPoint(@RequestBody PointRasterFetchDTO pointRasterFetchDTO) {
        return ResponseEntity.ok(modelExampleService.getRasterResultByPoint(pointRasterFetchDTO));
    }

    @PostMapping("/raster/line")
    public ResponseEntity<CommonResultVO> getRasterResultByLine(@RequestBody LineRasterFetchDTO lineRasterFetchDTO) {
        return ResponseEntity.ok(modelExampleService.getRasterResultByLine(lineRasterFetchDTO));
    }

    @PostMapping("/superResolution")
    public ResponseEntity<CommonResultVO> getSRResultByBand(@RequestBody SRBandDTO SRBandDTO) {
        return ResponseEntity.ok(modelExampleService.getSRResultByBand(SRBandDTO));
    }

    @PostMapping("/superResolutionV2")
    public ResponseEntity<CommonResultVO> getSRResultByBandV2(@RequestBody SRBandDTO SRBandDTO) {
        return ResponseEntity.ok(modelExampleService.getSRResultByBandV2(SRBandDTO));
    }

    @PostMapping("/esrgan")
    public ResponseEntity<CommonResultVO> getESRGANResultByBand(@RequestBody SRBandDTO SRBandDTO) {
        return ResponseEntity.ok(modelExampleService.getESRGANResultByBand(SRBandDTO));
    }
}
