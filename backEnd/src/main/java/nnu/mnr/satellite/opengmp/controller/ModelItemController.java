package nnu.mnr.satellite.opengmp.controller;

import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.opengmp.model.dto.ResourcePageDTO;
import nnu.mnr.satellite.opengmp.model.po.ModelItem;
import nnu.mnr.satellite.opengmp.model.vo.ModelItemVO;
import nnu.mnr.satellite.opengmp.service.ModelItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/31 10:18
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/model")
public class ModelItemController {

    private final ModelItemService modelItemService;

    public ModelItemController(ModelItemService modelItemService) {
        this.modelItemService = modelItemService;
    }

    @GetMapping("/{mid}")
    public ResponseEntity<ModelItemVO> getModelItemById(@PathVariable String mid) {
        return ResponseEntity.ok(modelItemService.getModelItemById(mid));
    }

    @PostMapping("/models")
    public ResponseEntity<CommonResultVO> getModelItems(@RequestBody ResourcePageDTO resourcePageDTO) {
        return ResponseEntity.ok(modelItemService.getResourceModelList(resourcePageDTO));
    }

    @PostMapping("/methods")
    public ResponseEntity<CommonResultVO> getMethodItems(@RequestBody ResourcePageDTO resourcePageDTO) {
        return ResponseEntity.ok(modelItemService.getResourceMethodList(resourcePageDTO));
    }

}
