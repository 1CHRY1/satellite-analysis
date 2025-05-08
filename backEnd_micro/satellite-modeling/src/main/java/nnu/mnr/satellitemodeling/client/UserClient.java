package nnu.mnr.satellitemodeling.client;

import nnu.mnr.satellitemodeling.model.po.user.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/21 17:19
 * @Description:
 */

@FeignClient(name = "satellite-user", contextId = "userClient")
public interface UserClient {

    @GetMapping("/api/v1/user/internal/validation")
    Boolean validateUser(@RequestHeader("X-User-Id") String userId);

    @GetMapping("/api/v1/user/internal/id/{userId}")
    User getUserById(@PathVariable String userId);

}
