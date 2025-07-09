package nnu.mnr.satellite.service.optimization;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 优化上下文 - 遵循SRP原则，封装优化所需的所有上下文信息
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Data
@Builder
public class OptimizationContext {
    
    /**
     * 项目ID
     */
    private String projectId;
    
    /**
     * 原始代码内容
     */
    private String originalCode;
    
    /**
     * Docker容器ID
     */
    private String containerId;
    
    /**
     * 优化配置参数
     */
    private Map<String, Object> configuration;
    
    /**
     * 优化模式
     */
    private OptimizationMode mode;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 优化模式枚举
     */
    public enum OptimizationMode {
        /**
         * 自动模式 - 系统自动选择最佳优化策略
         */
        AUTO,
        
        /**
         * 保守模式 - 只进行安全的优化
         */
        CONSERVATIVE,
        
        /**
         * 激进模式 - 进行更多的优化尝试
         */
        AGGRESSIVE
    }
} 