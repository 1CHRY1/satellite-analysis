package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.vector.VectorPageDTO;
import nnu.mnr.satellite.model.dto.admin.vector.VectorUpdateDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminVectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/api/v1/vector")
public class AdminVectorController {

    @Autowired
    private AdminVectorService adminVectorService;

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getVectorInfoPage(@RequestBody VectorPageDTO vectorPageDTO) throws Exception {
        return ResponseEntity.ok(adminVectorService.getVectorInfoPage(vectorPageDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateVector(@RequestBody VectorUpdateDTO vectorUpdateDTO){
        return ResponseEntity.ok(adminVectorService.updateVector(vectorUpdateDTO));
    }
}
