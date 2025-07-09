package nnu.mnr.satellite.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Docker模式配置
 * 用于控制本地和远程Docker模式的选择
 *
 * @Author: Assistant
 * @Date: 2025/7/9
 * @Description: Docker模式配置管理
 */
@Data
@Component
@ConfigurationProperties(prefix = "docker")
public class DockerModeConfig {

    /**
     * Docker模式：local（本地Docker）或 remote（远程服务器）
     */
    private String mode = "remote";

    /**
     * 本地Docker配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * 远程服务器配置
     */
    private DefaultServerConfig defaultServer = new DefaultServerConfig();

    /**
     * 兼容性配置
     */
    private String localPath;
    private String serverDir;
    private String workDir;

    /**
     * 是否为本地模式
     */
    public boolean isLocalMode() {
        return "local".equalsIgnoreCase(mode);
    }

    /**
     * 是否为远程模式
     */
    public boolean isRemoteMode() {
        return "remote".equalsIgnoreCase(mode);
    }

    /**
     * 本地Docker配置
     */
    @Data
    public static class LocalConfig {
        private boolean enabled = true;
        private String localPath;
        private String workDir;
    }

    /**
     * 远程服务器配置
     */
    @Data
    public static class DefaultServerConfig {
        private String host;
        private String port;
        private String username;
        private String password;
    }
} 