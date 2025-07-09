package nnu.mnr.satellite.service.optimization;

import nnu.mnr.satellite.model.po.modeling.Project;

/**
 * 代码执行策略接口
 * 遵循ISP原则 - 专注于执行计划创建职责
 * 
 * @author zzw
 * @date 2025/01/20
 */
public interface CodeExecutionStrategy {
    
    /**
     * 创建执行计划
     * @param project 项目信息
     * @param result 优化结果
     * @return 执行计划
     */
    ExecutionPlan createExecutionPlan(Project project, OptimizationResult result);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 获取策略优先级，数值越小优先级越高
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
} 