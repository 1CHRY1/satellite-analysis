package nnu.mnr.satellite.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Ray优化配置类
 * 
 * @Author: zzw
 * @Date: 2025/7/2 11:25
 * @Description: Ray优化相关配置管理
 */
@Configuration
@ConfigurationProperties(prefix = "ray.optimization")
@Data
public class RayOptimizationConfig {

    /**
     * 是否启用Ray优化
     */
    private boolean enabled = true;

    /**
     * 优化超时时间（秒）
     */
    private int timeout = 30;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    @Data
    public static class Cache {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;

        /**
         * 缓存过期时间（分钟）
         */
        private int expireMinutes = 10;

        /**
         * 最大缓存条目数
         */
        private int maxSize = 1000;
    }

    /**
     * 模型服务器配置
     */
    @Configuration
    @ConfigurationProperties(prefix = "modelServer")
    @Data
    public static class ModelServerConfig {
        private String address = "http://localhost:5002";
        private int timeout = 60000;
        private ConnectionPool connectionPool = new ConnectionPool();
        private RayEndpoints ray = new RayEndpoints();

        @Data
        public static class ConnectionPool {
            private int maxTotal = 20;
            private int maxPerRoute = 5;
        }

        @Data
        public static class RayEndpoints {
            private String optimizationEndpoint = "/v0/ray/optimize";
            private String statusEndpoint = "/v0/ray/status";
            private String metricsEndpoint = "/v0/ray/metrics";
        }
    }

    /**
     * 安全配置
     */
    @Configuration
    @ConfigurationProperties(prefix = "security.code-execution")
    @Data
    public static class SecurityConfig {
        private long maxFileSize = 10 * 1024 * 1024; // 10MB
        private int maxExecutionTime = 300; // 5分钟
        private List<String> allowedImports;
        private List<String> forbiddenOperations;
    }

    /**
     * 监控配置
     */
    @Configuration
    @ConfigurationProperties(prefix = "monitoring.ray")
    @Data
    public static class MonitoringConfig {
        private boolean enabled = true;
        private int metricsInterval = 30; // 秒
        private AlertThresholds alertThresholds = new AlertThresholds();

        @Data
        public static class AlertThresholds {
            private double errorRate = 0.1;
            private double timeoutRate = 0.05;
        }
    }

    /**
     * 配置RestTemplate Bean
     */
    @Bean("rayOptimizationRestTemplate")
    public RestTemplate restTemplate(ModelServerConfig modelServerConfig) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(modelServerConfig.getTimeout());
        factory.setReadTimeout(modelServerConfig.getTimeout());
        
        return new RestTemplate(factory);
    }
} 