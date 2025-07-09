package nnu.mnr.satellite.config.optimization;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ray优化配置属性类
 * 遵循CoC原则 - 约定优于配置，提供合理的默认值
 * 
 * @author zzw
 * @date 2025/01/20
 */
@ConfigurationProperties(prefix = "ray.optimization")
@Component
@Data
public class RayOptimizationProperties {
    
    /**
     * 是否启用Ray优化
     */
    private boolean enabled = true;
    
    /**
     * Ray使用的CPU数量 (1-64)
     */
    private int cpuCount = 4;
    
    /**
     * Ray使用的内存量(MB) (512-16384)
     */
    private int memoryMB = 2048;
    
    /**
     * 优化超时时间(秒) (10-300)
     */
    private int timeout = 60;
    
    /**
     * 优化失败时是否回退到原始代码
     */
    private boolean fallbackOnError = true;
    
    /**
     * 分析模式 (auto/conservative/aggressive)
     */
    private String analysisMode = "auto";
    
    /**
     * 日志级别
     */
    private String logLevel = "INFO";
    
    /**
     * Ray临时目录
     */
    private String tempDir = "/tmp/ray";
    
    /**
     * 是否生成优化报告
     */
    private boolean enableOptimizationReport = true;
    
    /**
     * 最大重试次数 (0-5)
     */
    private int maxRetries = 2;
    
    /**
     * 是否启用优化缓存
     */
    private boolean enableCaching = true;
    
    /**
     * 缓存过期时间(分钟) (1-1440)
     */
    private int cacheTtlMinutes = 60;
    
    /**
     * 获取Ray初始化参数
     */
    public String getRayInitParams() {
        return String.format("num_cpus=%d,object_store_memory=%d000000", cpuCount, memoryMB);
    }
    
    /**
     * 检查配置是否有效
     */
    public boolean isValid() {
        return enabled && cpuCount > 0 && memoryMB > 0 && timeout > 0;
    }

    /**
     * Ray Dashboard配置
     */
    private Dashboard dashboard = new Dashboard();

    /**
     * Ray Dashboard配置类
     */
    @Data
    public static class Dashboard {
        /**
         * 是否启用Ray Dashboard访问
         */
        private boolean enabled = false;

        /**
         * Ray Dashboard端口
         */
        private int port = 8265;

        /**
         * 宿主机映射端口
         */
        private int hostPort = 8265;
    }
} 