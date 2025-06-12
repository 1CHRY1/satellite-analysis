package nnu.mnr.satellite.controller.resources;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CaseInfoVO;
import nnu.mnr.satellite.service.resources.CaseDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.baomidou.mybatisplus.core.metadata.IPage;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;
import nnu.mnr.satellite.model.po.resources.Case;

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

    @GetMapping("/page")
    public ResponseEntity<CommonResultVO> getCasePage(@ModelAttribute CasePageDTO casePageDTO) {
        return ResponseEntity.ok(caseDataService.getCasePage(casePageDTO));
    }

}
