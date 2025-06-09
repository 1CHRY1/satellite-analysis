package nnu.mnr.satellite.controller.resources;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.service.resources.CaseDataService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Resource
    private CaseDataService caseDataService;


}
