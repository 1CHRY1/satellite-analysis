package nnu.mnr.satellite.controller.modeling;


import io.jsonwebtoken.JwtException;
import nnu.mnr.satellite.model.dto.modeling.MethlibCasePageDTO;
import nnu.mnr.satellite.model.dto.modeling.MethlibPageDTO;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.modeling.MethlibService;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v3/methlib")
public class MethlibController {

    @Autowired
    private MethlibService methlibService;

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getMethlibPage(@RequestBody MethlibPageDTO methlibPageDTO){
        return ResponseEntity.ok(methlibService.getMethlibPage(methlibPageDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResultVO> getMethlibById(@PathVariable String id){
        return ResponseEntity.ok(methlibService.getMethlibById(id));
    }

    @GetMapping("/tag/all")
    public ResponseEntity<CommonResultVO> getAllTags(){
        return ResponseEntity.ok(methlibService.getAllTags());
    }

    @PostMapping("/invoke/{methlibId}")
    public ResponseEntity<CommonResultVO> invokeMethlib(@PathVariable("methlibId") Integer methlibId,
                                                        @RequestBody Map<String, Object> params,
                                                        @RequestHeader(value = "Authorization", required = false) String authorizationHeader){
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok(methlibService.invokeMethlib(methlibId, params, userId));
    }

    @PostMapping("/case/page")
    public ResponseEntity<CommonResultVO> getMethlibCasePage(@RequestBody MethlibCasePageDTO methlibCasePageDTO,
                                                      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String userId;
        try {
            userId = IdUtil.parseUserIdFromAuthHeader(authorizationHeader);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok(methlibService.getMethlibCasePage(methlibCasePageDTO, userId));
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<CommonResultVO> getCaseByCaseId(@PathVariable("caseId") String caseId) {
        return ResponseEntity.ok(methlibService.getCaseByCaseId(caseId));
    }

    @GetMapping("/case/result/{caseId}")
    public ResponseEntity<CommonResultVO> getResultByCaseId(@PathVariable("caseId") String caseId) {
        return ResponseEntity.ok(methlibService.getResultByCaseId(caseId));
    }
}
