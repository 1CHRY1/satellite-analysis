package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.case_.CaseDeleteDTO;
import nnu.mnr.satellite.model.dto.modeling.MethlibCasePageDTO;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;
import nnu.mnr.satellite.model.po.modeling.Methlib;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/case")
public class AdminCaseController {

    @Autowired
    private AdminCaseService adminCaseService;

    @PostMapping({"/page", "/page/{userId}"})
    public ResponseEntity<CommonResultVO> getCasePage(@RequestBody CasePageDTO casePageDTO,
                                                      @PathVariable(required = false) String userId){
        return ResponseEntity.ok(adminCaseService.getCasePage(casePageDTO, userId));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteCase(@RequestBody CaseDeleteDTO caseDeleteDTO){
        return ResponseEntity.ok(adminCaseService.deleteCase(caseDeleteDTO));
    }

    @PostMapping({"/methlib/page", "/methlib/page/{userId}"})
    public ResponseEntity<CommonResultVO> getMethlibCasePage(@RequestBody MethlibCasePageDTO methlibCasePageDTO,
                                                             @PathVariable(required = false) String userId){
        return ResponseEntity.ok(adminCaseService.getMethlibCasePage(methlibCasePageDTO, userId));
    }

    @DeleteMapping("/methlib/delete")
    public ResponseEntity<CommonResultVO> deleteMethlibCase(@RequestBody CaseDeleteDTO caseDeleteDTO){
        return ResponseEntity.ok(adminCaseService.deleteMethlibCase(caseDeleteDTO));
    }
}
