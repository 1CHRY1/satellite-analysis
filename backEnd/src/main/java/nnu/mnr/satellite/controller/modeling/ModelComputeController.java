package nnu.mnr.satellite.controller.modeling;

import nnu.mnr.satellite.service.modeling.ModelComputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 14:19
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/model/compute")
public class ModelComputeController {

    @Autowired
    ModelComputeService modelComputeService;

}
