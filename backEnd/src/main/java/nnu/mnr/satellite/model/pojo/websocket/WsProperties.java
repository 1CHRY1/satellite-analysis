package nnu.mnr.satellite.model.pojo.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/18 22:39
 * @Description:
 */

@ConfigurationProperties(prefix = WsProperties.WEBSOCKET_PREFIX)
@Data
public class WsProperties {

    public static final String WEBSOCKET_PREFIX = "netty.websocket";

    private Integer port;

    private String applicationName;

    private String nacosAddr;

    private Integer bossThreadNums = 1;

    private Integer workerThreadNums = 2;

    /**
     * 连接超时时间
     */
    private Integer connectTimeout = 150000;
    /**
     * TCP 连接的请求队列的最大长度,默认128
     */
    private Integer backLog = 128;

    /**
     * 消息是否立即发送
     */
    private boolean tcpNoDelay = true;

    /**
     * 心跳读超时时间
     */
    private Integer readerIdleTimeSeconds = 60;

    /**
     * 心跳写超时时间
     */
    private Integer writerIdleTimeSeconds = 60;

    private Integer allIdleTimeSeconds = 60;

    /**
     * 限制可以接收的最大字节数。当数据包大小超过 maxContentLength 时将拒绝接收该数据包并关闭连接
     */
    private Integer maxContentLength = 65536;

}
