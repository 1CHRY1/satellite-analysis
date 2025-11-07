package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("admin/api/v1/dashboard")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<CommonResultVO> getStats() throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }

    @GetMapping("/activity")
    public ResponseEntity<CommonResultVO> getActivity() {
        return ResponseEntity.ok(adminDashboardService.getActivity());
    }
}
