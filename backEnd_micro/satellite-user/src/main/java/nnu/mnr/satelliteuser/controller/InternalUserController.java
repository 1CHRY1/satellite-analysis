package nnu.mnr.satelliteuser.controller;

import nnu.mnr.satelliteuser.model.po.User;
import nnu.mnr.satelliteuser.repository.IUserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/21 16:41
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/user/internal")
public class InternalUserController {

    @Autowired
    IUserRepo userRepo;

    @GetMapping("/validation")
    public ResponseEntity<Boolean> validateUser(@RequestHeader("X-User-Id") String userId) {
        User user = userRepo.selectById(userId);
        return ResponseEntity.ok(user != null);
    }

}
