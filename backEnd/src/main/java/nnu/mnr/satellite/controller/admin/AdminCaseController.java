package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.case_.CaseDeleteDTO;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/api/v1/case")
public class AdminCaseController {

    @Autowired
    private AdminCaseService adminCaseService;

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getCasePage(CasePageDTO casePageDTO){
        return ResponseEntity.ok(adminCaseService.getCasePage(casePageDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteCase(CaseDeleteDTO caseDeleteDTO){
        return ResponseEntity.ok(adminCaseService.deleteCase(caseDeleteDTO));
    }
}
