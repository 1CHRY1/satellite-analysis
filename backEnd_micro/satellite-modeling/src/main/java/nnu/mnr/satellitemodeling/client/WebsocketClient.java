package nnu.mnr.satellitemodeling.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/22 17:34
 * @Description:
 */

@FeignClient(name = "satellite-websocket", contextId = "websocketClient")
public interface WebsocketClient {

    @PostMapping ("/api/v1/websocket/internal/send/{userId}/{projectId}")
    Boolean sendMessage(@PathVariable String userId, @PathVariable String projectId, @RequestBody String message);

}
