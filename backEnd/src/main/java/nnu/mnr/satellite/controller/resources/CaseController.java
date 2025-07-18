package nnu.mnr.satellite.controller.resources;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.resources.CaseDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @name: CaseController
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/4/2025 8:13 PM
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v1/data/case")
@Slf4j
public class CaseController {

    private final CaseDataService caseDataService;
    public CaseController(CaseDataService caseDataService) {
        this.caseDataService = caseDataService;
    }

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getCasePage(@RequestBody CasePageDTO casePageDTO) {
        return ResponseEntity.ok(caseDataService.getCasePage(casePageDTO));
    }

    @GetMapping("/boundary/{caseId}")
    public ResponseEntity<CommonResultVO> getCaseBoundaryByCaseId(@PathVariable("caseId") String caseId) {
        return ResponseEntity.ok(caseDataService.getCaseBoundaryByCaseId(caseId));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CommonResultVO> getCaseByCaseId(@PathVariable("caseId") String caseId) {
        return ResponseEntity.ok(caseDataService.getCaseByCaseId(caseId));
    }

    @GetMapping("result/{caseId}")
    public ResponseEntity<CommonResultVO> getResultByCaseId(@PathVariable("caseId") String caseId) {
        return ResponseEntity.ok(caseDataService.getResultByCaseId(caseId));
    }

}
