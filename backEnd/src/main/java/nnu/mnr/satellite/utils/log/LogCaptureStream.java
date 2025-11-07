package nnu.mnr.satellite.utils.log;

import nnu.mnr.satellite.service.websocket.WebSocketService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// 捕获日志工具类
public class LogCaptureStream extends OutputStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private static final BlockingQueue<String> logHistory = new LinkedBlockingQueue<>(10000); // 最多存储 10000 行
    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
        if (b == '\n') {
            String logLine = buffer.toString(StandardCharsets.UTF_8);
            buffer.reset();
            storeLog(logLine);
            pushLogToFrontend(logLine);
        }
        originalOut.write(b); // 可选：同时输出到控制台
    }

    // 存储日志到环形缓冲区
    private void storeLog(String logLine) {
        if (logHistory.remainingCapacity() == 0) {
            logHistory.poll(); // 如果队列满了，移除最早的日志
        }
        logHistory.offer(logLine);
    }

    // 获取历史日志（用于 WebSocket 连接时发送）
    public static String getHistoryLogs(int lines) {
        StringBuilder sb = new StringBuilder();
        List<String> logs = new ArrayList<>(logHistory); // 转为 List 方便按索引截取
        int start = Math.max(0, logs.size() - lines); // 计算起始位置
        for (int i = start; i < logs.size(); i++) {
            sb.append(logs.get(i));
        }
        return sb.toString();
    }

    private void pushLogToFrontend(String logLine) {
        // 实际推送逻辑（通过 WebSocket/SSE）
        WebSocketService.broadcastLog(logLine); // 示例：调用 WebSocket 控制器
    }
}
