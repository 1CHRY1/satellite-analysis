package nnu.mnr.satellitewebsocket.controller;

import nnu.mnr.satellitewebsocket.service.ModelSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/22 19:27
 * @Description:
 */

@RestController
@RequestMapping("/api/v1/websocket/internal")
public class InternalWebsocketController {

    @Autowired
    private ModelSocketService modelSocketService;

    @PostMapping("/send/{userId}/{projectId}")
    public ResponseEntity<Void> sendMessage(@PathVariable String userId, @PathVariable String projectId, @RequestBody String message) {
        modelSocketService.sendMessageByProject(userId, projectId, message);
        return ResponseEntity.ok().build();
    }

}
