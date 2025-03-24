package nnu.mnr.satellite.service.websocket;

import nnu.mnr.satellite.nettywebsocket.annotations.PathParam;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.nettywebsocket.annotations.*;
import nnu.mnr.satellite.nettywebsocket.socket.Session;
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


@WsServerEndpoint(value = "/model/websocket/userId/{userId}/projectId/{projectId}")
@Service
@Slf4j
public class ModelSocketService {

    private Session session;

    private static ConcurrentHashMap<String, Map<String, Session>> sessionPool = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId,  @PathParam(value = "projectId") String projectId) {
        sessionPool.putIfAbsent(projectId, new ConcurrentHashMap<>());
        Map<String, Session> sessions = sessionPool.get(projectId);
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
        for (Map.Entry<String, Map<String,Session>> entry : sessionPool.entrySet()) {
            if (entry.getKey().equals(projectId)) {
                Map<String,Session> sessions = entry.getValue();
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
        Map<String,Session> sessions = sessionPool.get(projectId);
        if (sessions != null) {
            for (Map.Entry<String, Session> map : sessions.entrySet()) {
                map.getValue().sendText(message);
            }
        }
    }

}
