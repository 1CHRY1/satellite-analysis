package nnu.mnr.satelliteopengmp.client;

import feign.Headers;
import nnu.mnr.satelliteopengmp.config.web.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/21 17:19
 * @Description:
 */

@FeignClient(name = "satellite-user", contextId = "userClient", configuration = FeignConfig.class)
public interface UserClient {

    @GetMapping("/api/v1/user/internal/validation")
    Boolean validateUser(@RequestHeader("X-User-Id") String userId);
}
