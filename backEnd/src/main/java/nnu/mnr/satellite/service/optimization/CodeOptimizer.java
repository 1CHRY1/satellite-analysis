package nnu.mnr.satellite.service.optimization;

/**
 * 代码优化器抽象接口
 * 遵循ISP原则 - 接口隔离，职责单一
 * 
 * @author zzw
 * @date 2025/01/20
 */
public interface CodeOptimizer {
    
    /**
     * 优化代码
     * @param context 优化上下文
     * @return 优化结果
     */
    OptimizationResult optimize(OptimizationContext context);
    
    /**
     * 检查是否支持该类型的优化
     * @param context 优化上下文
     * @return 是否支持
     */
    boolean supports(OptimizationContext context);
    
    /**
     * 获取优化器名称
     * @return 优化器名称
     */
    String getName();
    
    /**
     * 获取优化器优先级，数值越小优先级越高
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
} 