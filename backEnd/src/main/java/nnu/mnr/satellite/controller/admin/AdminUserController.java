package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.user.UserDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.user.UserInsertDTO;
import nnu.mnr.satellite.model.dto.admin.user.UserPageDTO;
import nnu.mnr.satellite.model.dto.admin.user.UserUpdateDTO;
import nnu.mnr.satellite.model.dto.user.AvatarUploadDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getUserInfoPage(@RequestBody UserPageDTO userPageDTO) throws Exception {
        return ResponseEntity.ok(adminUserService.getUserInfoPage(userPageDTO));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CommonResultVO> getUserInfoById(@PathVariable String userId) throws Exception {
        return ResponseEntity.ok(adminUserService.getUserInfoById(userId));
    }

    @PutMapping("/insert")
    public ResponseEntity<CommonResultVO> insertUser(@RequestBody UserInsertDTO userInsertDTO) throws Exception {
        return ResponseEntity.ok(adminUserService.insertUser(userInsertDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateUser(@RequestBody UserUpdateDTO userUpdateDTO) throws Exception {
        return ResponseEntity.ok(adminUserService.updateUser(userUpdateDTO));
    }

    @PostMapping("/avatar/upload")
    public ResponseEntity<CommonResultVO> uploadAvatar(@ModelAttribute AvatarUploadDTO avatarUploadDTO) {
        return ResponseEntity.ok(adminUserService.uploadAvatar(avatarUploadDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteUser(@RequestBody UserDeleteDTO userDeleteDTO) throws Exception {
        return ResponseEntity.ok(adminUserService.deleteUser(userDeleteDTO));
    }

}
