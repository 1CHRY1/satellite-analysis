package nnu.mnr.satellite.service.websocket;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.nettywebsocket.annotations.*;
import nnu.mnr.satellite.nettywebsocket.socket.Session;
import nnu.mnr.satellite.utils.log.LogCaptureStream;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@WsServerEndpoint(value = "/log/{lines}")
@Service
@Slf4j
public class WebSocketService {
    private static final CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("lines") String lines) {
        sessions.add(session); // 连接建立时添加
        // 发送历史日志
        String historyLogs = LogCaptureStream.getHistoryLogs(Integer.parseInt(lines));
        if (!historyLogs.isEmpty()) {
            session.sendText("======== HISTORY LOGS START ========");
            session.sendText(historyLogs);
            session.sendText("======== HISTORY LOGS END ========");
        }
        log.info("WebSocket opened: {}, requested last {} lines", session.getId(), lines);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session); // 连接关闭时移除
        log.info("WebSocket closed: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error for session {}: {}", session.getId(), throwable.getMessage());
        session.close();
        sessions.remove(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("Received message from {}: {}", session.getId(), message);
    }

    public static void broadcastLog(String logMessage) {
        sessions.forEach(session -> {
            if (session == null || !session.isOpen()) {
                sessions.remove(session); // 清理无效连接
                return;
            }
            try {
                session.sendText(logMessage);
            } catch (Exception e) {
                log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                // 尝试关闭异常连接
                session.close();
                sessions.remove(session);
            }
        });
    }
}
