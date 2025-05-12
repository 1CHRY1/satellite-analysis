package nnu.mnr.satellite.controller.modeling;

import nnu.mnr.satellite.model.dto.modeling.NdviFetchDTO;
import nnu.mnr.satellite.model.dto.modeling.NoCloudFetchDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.modeling.ModelExampleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<CommonResultVO> getNoCloudByRegion(NoCloudFetchDTO noCloudFetchDTO) {
        return ResponseEntity.ok(modelExampleService.getNoCloudByRegion(noCloudFetchDTO));
    }

    @PostMapping("/ndvi/point")
    public ResponseEntity<CommonResultVO> getNdviByRegion(NdviFetchDTO ndviFetchDTO) {
        return ResponseEntity.ok(modelExampleService.getNDVIByPoint(ndviFetchDTO));
    }

}
