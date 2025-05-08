package nnu.mnr.satellite.controller.modeling;

import nnu.mnr.satellite.model.dto.modeling.NdviDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
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
@RequestMapping("/api/v1/modeling/case")
public class ModelComputeController {

    @Autowired
    ModelServerService modelServerService;

    // Common Controllers ******************************
    @GetMapping("/status/caseId/{caseId}")
    public ResponseEntity<CommonResultVO> getModelCaseStatus(@PathVariable String caseId) {
        return ResponseEntity.ok(modelServerService.getModelCaseStatusById(caseId));
    }

    @GetMapping("/result/caseId/{caseId}")
    public ResponseEntity<CommonResultVO> getModelCaseResultById(@PathVariable String caseId) {
        return ResponseEntity.ok(modelServerService.getModelCaseResultById(caseId));
    }

    @GetMapping("/result/tif/caseId/{caseId}")
    public ResponseEntity<CommonResultVO> getModelCaseTifResultById(@PathVariable String caseId) {
        return ResponseEntity.ok(modelServerService.getModelCaseTifResultById(caseId));
    }

    @GetMapping("/data/tif/caseId/{caseId}")
    public ResponseEntity<byte[]> getTifDataById(@PathVariable String caseId) {
        byte[] modelData = modelServerService.getTifDataById(caseId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/tiff"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(modelData);
    }

}
