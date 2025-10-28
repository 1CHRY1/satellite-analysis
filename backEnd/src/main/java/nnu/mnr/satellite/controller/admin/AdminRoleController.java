package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.role.RoleDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.role.RoleInsertDTO;
import nnu.mnr.satellite.model.dto.admin.role.RolePageDTO;
import nnu.mnr.satellite.model.dto.admin.role.RoleUpdateDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/role")
public class AdminRoleController {

    @Autowired
    private AdminRoleService adminRoleService;

    @GetMapping("")
    public ResponseEntity<CommonResultVO> getAllRoleInfo() throws Exception {
        return ResponseEntity.ok(adminRoleService.getAllRoleInfo());
    }

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getRoleInfoPage(@RequestBody RolePageDTO rolePageDTO) throws Exception {
        return ResponseEntity.ok(adminRoleService.getRoleInfoPage(rolePageDTO));
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<CommonResultVO> getRoleInfoById(@PathVariable("roleId") String roleId) throws Exception {
        return ResponseEntity.ok(adminRoleService.getRoleInfoById(roleId));
    }

    @PutMapping("/insert")
    public ResponseEntity<CommonResultVO> insertRole(@RequestBody RoleInsertDTO roleInsertDTO) throws Exception {
        return ResponseEntity.ok(adminRoleService.insertRole(roleInsertDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateRole(@RequestBody RoleUpdateDTO roleUpdateDTO) throws Exception {
        return ResponseEntity.ok(adminRoleService.updateRole(roleUpdateDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteRole(@RequestBody RoleDeleteDTO roleDeleteDTO) throws Exception {
        return ResponseEntity.ok(adminRoleService.deleteRole(roleDeleteDTO));
    }



}
