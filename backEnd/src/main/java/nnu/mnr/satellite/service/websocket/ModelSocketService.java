package nnu.mnr.satellite.service.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.annotations.websocket.WsEndPoint;
import nnu.mnr.satellite.model.pojo.websocket.WsSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/18 21:13
 * @Description:
 */


@WsEndPoint(value = "/ModelSocketService/{userId}/{projectId}")
@Service
@Slf4j
public class ModelSocketService {

    private WsSession session;

    private static ConcurrentHashMap<String, Map<String, WsSession>> sessionPool = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(WsSession session, @PathParam(value = "userId") String userId,  @PathParam(value = "projectId") String projectId) {
        sessionPool.putIfAbsent(projectId, new ConcurrentHashMap<>());
        Map<String, WsSession> sessions = sessionPool.get(projectId);
        sessions.put(userId, session);

        this.session = session;
        log.info("User {} Joined Project {}. Online People: {}", userId, projectId, sessions.size());
    }

    @OnError
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @OnClose
    public void onClose(@PathParam(value = "userId") String userId, @PathParam(value = "projectId") String projectId) {
        for (Map.Entry<String, Map<String,WsSession>> entry : sessionPool.entrySet()) {
            if (entry.getKey().equals(projectId)) {
                Map<String,WsSession> sessions = entry.getValue();
                sessions.remove(userId);
                log.info("User {} of Project: {} Disconnected, Online People: {}", userId, entry.getKey(), entry.getValue().size());
            }
        }
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("Received from Client：{}", message);
    }

    public void sendMessageByProject(String projectId, String message) {
        log.info("Project: {}, Sending: {}", projectId, message);
        Map<String,WsSession> sessions = sessionPool.get(projectId);
        if (sessions != null) {
            for (Map.Entry<String, WsSession> map : sessions.entrySet()) {
                map.getValue().sendText(message);
            }
        }
    }

}
