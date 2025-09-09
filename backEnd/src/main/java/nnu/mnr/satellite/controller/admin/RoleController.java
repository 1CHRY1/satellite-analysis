package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("")
    public ResponseEntity<CommonResultVO> getAllRoleInfo() throws Exception {
        return ResponseEntity.ok(roleService.getAllRoleInfo());
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<CommonResultVO> getRoleInfoById(@PathVariable("roleId") String roleId) throws Exception {
        return ResponseEntity.ok(roleService.getRoleInfoById(roleId));
    }
}
