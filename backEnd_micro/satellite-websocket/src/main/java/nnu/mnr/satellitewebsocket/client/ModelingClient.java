package nnu.mnr.satellitewebsocket.client;

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

@FeignClient(name = "satellite-modeling", contextId = "modelingClient")
public interface ModelingClient {

    @GetMapping("/api/v1/modeling/internal/validation")
    Boolean validateProject(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-Project-Id") String projectId);

}
