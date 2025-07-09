package nnu.mnr.satellite.service.optimization;

/**
 * 执行环境管理器接口
 * 遵循ISP原则 - 专注于环境管理职责
 * 
 * @author zzw
 * @date 2025/01/20
 */
public interface ExecutionEnvironmentManager {
    
    /**
     * 准备优化执行环境
     * @param containerId Docker容器ID
     * @param context 优化上下文
     */
    void prepareEnvironment(String containerId, OptimizationContext context);
    
    /**
     * 清理优化执行环境
     * @param containerId Docker容器ID
     * @param context 优化上下文
     */
    void cleanupEnvironment(String containerId, OptimizationContext context);
    
    /**
     * 检查环境是否就绪
     * @param containerId Docker容器ID
     * @param context 优化上下文
     * @return 环境是否就绪
     */
    default boolean isEnvironmentReady(String containerId, OptimizationContext context) {
        return true;
    }
} 