package nnu.mnr.satelliteuser.controller;

import nnu.mnr.satelliteuser.model.dto.UserInfoDTO;
import nnu.mnr.satelliteuser.model.dto.UserLoginDTO;
import nnu.mnr.satelliteuser.model.dto.UserRegisterDTO;
import nnu.mnr.satelliteuser.model.vo.CommonResultVO;
import nnu.mnr.satelliteuser.model.vo.UserVO;
import nnu.mnr.satelliteuser.service.UserService;
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
