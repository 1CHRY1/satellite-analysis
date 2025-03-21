package nnu.mnr.satellite.controller.modeling;

import com.alibaba.fastjson2.JSONArray;
import nnu.mnr.satellite.model.dto.modeling.NdviDTO;
import nnu.mnr.satellite.service.modeling.ModelServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 14:19
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/model")
public class ModelComputeController {

    @Autowired
    ModelServerService modelServerService;

    // Common Controllers ******************************
    @GetMapping("/status/caseId/{caseId}")
    public ResponseEntity<String> getModelCaseStatus(@PathVariable String caseId) {
        return ResponseEntity.ok(modelServerService.getModelCaseStatusById(caseId));
    }

    @GetMapping("/data/dataId/{dataId}")
    public ResponseEntity<byte[]> getModelDataById(@PathVariable String dataId) {
        byte[] modelData = modelServerService.getModelDataById(dataId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/tiff"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(modelData);
    }

    // Business Controllers ******************************
    @PostMapping("/compute/ndvi/{modelName}")
    public ResponseEntity<String> getNDVIPoints(@PathVariable String modelName, @RequestBody NdviDTO ndviDTO) {
        return switch (modelName) {
            case "point" -> ResponseEntity.ok(modelServerService.getNDVIByPoint(ndviDTO));
            case "area" -> ResponseEntity.ok(modelServerService.getNDVIByPolygon(ndviDTO));
            default -> ResponseEntity.ok("NO SUCH MODEL");
        };
    }

}
