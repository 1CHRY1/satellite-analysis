package nnu.mnr.satellite.service.optimization;

import lombok.Builder;
import lombok.Data;

/**
 * 代码优化结果数据类
 * 遵循SRP原则 - 封装整个优化流程的结果信息
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Data
@Builder
public class CodeOptimizationOutcome {
    
    /**
     * 优化结果
     */
    private OptimizationResult optimizationResult;
    
    /**
     * 执行计划
     */
    private ExecutionPlan executionPlan;
    
    /**
     * 整个流程是否成功
     */
    private boolean successful;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 处理耗时（毫秒）
     */
    private long processingTimeMs;
    
    /**
     * 是否使用了优化代码
     */
    public boolean isUsingOptimizedCode() {
        return successful && 
               optimizationResult != null && 
               optimizationResult.hasOptimization() &&
               executionPlan != null &&
               executionPlan.isOptimized();
    }
    
    /**
     * 获取执行描述
     */
    public String getExecutionDescription() {
        if (executionPlan != null) {
            return executionPlan.getDescription();
        } else if (errorMessage != null) {
            return "优化失败: " + errorMessage;
        } else {
            return "优化状态未知";
        }
    }
    
    /**
     * 创建成功的结果
     */
    public static CodeOptimizationOutcome success(OptimizationResult optimizationResult, 
                                                 ExecutionPlan executionPlan,
                                                 long processingTime) {
        return CodeOptimizationOutcome.builder()
                .optimizationResult(optimizationResult)
                .executionPlan(executionPlan)
                .successful(true)
                .processingTimeMs(processingTime)
                .build();
    }
    
    /**
     * 创建失败的结果
     */
    public static CodeOptimizationOutcome failure(String errorMessage, 
                                                 OptimizationResult fallbackResult,
                                                 ExecutionPlan fallbackPlan,
                                                 long processingTime) {
        return CodeOptimizationOutcome.builder()
                .optimizationResult(fallbackResult)
                .executionPlan(fallbackPlan)
                .successful(false)
                .errorMessage(errorMessage)
                .processingTimeMs(processingTime)
                .build();
    }
} 