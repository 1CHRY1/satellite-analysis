package nnu.mnr.satelliteuser.controller;

import nnu.mnr.satelliteuser.model.po.User;
import nnu.mnr.satelliteuser.repository.IUserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/id/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userRepo.selectById(userId);
        return ResponseEntity.ok(user);
    }

}
