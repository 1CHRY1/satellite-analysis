package nnu.mnr.satellite.controller.user;

import nnu.mnr.satellite.model.dto.user.UserInfoDTO;
import nnu.mnr.satellite.model.dto.user.UserLoginDTO;
import nnu.mnr.satellite.model.dto.user.UserRegisterDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.user.UserVO;
import nnu.mnr.satellite.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 17:30
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<CommonResultVO> userRegister(@RequestBody UserRegisterDTO userRegisterDTO) {
        return ResponseEntity.ok(userService.userRegister(userRegisterDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResultVO> userLogin(@RequestBody UserLoginDTO userLoginDTO) {
        return ResponseEntity.ok(userService.userLogin(userLoginDTO));
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonResultVO> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(userService.refreshToken(refreshToken));
    }

    @GetMapping("/description/userId/{userId}")
    public ResponseEntity<UserVO> getUserDescription(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserVOById(userId));
    }

    @PutMapping("/description/userId/{userId}")
    public ResponseEntity<CommonResultVO> updateUserDescription(@RequestBody UserInfoDTO userInfoDTO) {
        return ResponseEntity.ok(userService.updateUserInfoById(userInfoDTO));
    }

}
