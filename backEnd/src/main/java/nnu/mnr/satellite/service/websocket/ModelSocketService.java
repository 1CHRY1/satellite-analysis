package nnu.mnr.satellite.service.websocket;

import nnu.mnr.satellite.nettywebsocket.annotations.PathParam;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.nettywebsocket.annotations.*;
import nnu.mnr.satellite.nettywebsocket.socket.Session;
import nnu.mnr.satellite.service.modeling.ProjectDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    private final int USR_MAX_SESSIONS = 10;

    @Autowired
    ProjectDataService projectDataService;

    private static ConcurrentHashMap<String, Map<String, Set<Session>>> sessionPool = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId,  @PathParam(value = "projectId") String projectId) {
        if ( !projectDataService.VerifyUserProject(userId, projectId) ) {
            session.sendText("Connection rejected: User Not Allowed");
            session.close();
            return;
        }
        sessionPool.putIfAbsent(projectId, new ConcurrentHashMap<>());
        Map<String, Set<Session>> sessions = sessionPool.get(projectId);
        Set<Session> userSessions = sessions
                .computeIfAbsent(userId, k -> Collections.synchronizedSet(new HashSet<>()));
        synchronized (userSessions) {
            if (userSessions.size() >= USR_MAX_SESSIONS) {
                session.sendText("Connection rejected: Maximum of " + USR_MAX_SESSIONS + " sessions reached.");
                session.close();
                log.info("User {} in Project {} connection rejected: session limit ({}) reached.",
                        userId, projectId, USR_MAX_SESSIONS);
                return;
            }
            if (!userSessions.contains(session)) {
                userSessions.add(session);
            }
            log.info("User {} Joined Project {}. Online Sessions for User: {}, Total Users: {}",
                    userId, projectId, userSessions.size(), sessions.size());
        }
    }

    @OnError
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, @PathParam(value = "userId") String userId, @PathParam(value = "projectId") String projectId) {
        Map<String, Set<Session>> sessions = sessionPool.get(projectId);
        if (sessions != null) {
            Set<Session> userSessions = sessions.get(userId);
            if (userSessions != null) {
                synchronized (userSessions) {
                    String closingSessionId = session.getId();
                    Session toRemove = null;
                    for (Session s : userSessions) {
                        if (s.getId().equals(closingSessionId)) {
                            toRemove = s;
                            break;
                        }
                    }
                    if (toRemove != null) {
                        userSessions.remove(toRemove);
                    } else {
                        log.warn("Session {} not found in pool", closingSessionId);
                        return;
                    }
                    if (userSessions.isEmpty()) {
                        sessions.remove(userId);
                    }
                    log.info("User {} of Project: {} Disconnected, Remaining Sessions: {}, Total Users: {}",
                            userId, projectId, userSessions.size(), sessions.size());
                }
            }
        }
    }

    @OnMessage
    public void onMessage(String message) {
//        log.info("Received from Client：{}", message);
    }

    public void sendMessageByProject(String userId, String projectId, String message) {
        log.info("User: {}, Project: {}, Sending: {}", userId, projectId, message);
        Map<String, Set<Session>> sessions = sessionPool.get(projectId);
        if (sessions != null) {
            Set<Session> userSessions = sessions.get(userId);
            if (userSessions != null) {
                synchronized (userSessions) {
                    for (Session s : userSessions) {
                        if (s.isOpen()) {
                            s.sendText(message); // 向该用户的所有设备发送消息
                        }
                    }
                }
            }
        }
    }
}
