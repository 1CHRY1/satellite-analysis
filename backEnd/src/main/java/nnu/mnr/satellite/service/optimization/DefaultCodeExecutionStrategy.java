package nnu.mnr.satellite.service.optimization;

import nnu.mnr.satellite.model.po.modeling.Project;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 默认代码执行策略
 * 遵循SRP原则 - 专注于执行计划创建
 * 遵循Strategy Pattern - 可替换的执行策略
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Component
@Primary
public class DefaultCodeExecutionStrategy implements CodeExecutionStrategy {
    
    @Override
    public ExecutionPlan createExecutionPlan(Project project, OptimizationResult result) {
        if (result.hasOptimization()) {
            return createOptimizedExecutionPlan(project, result);
        } else {
            return createOriginalExecutionPlan(project, result);
        }
    }
    
    @Override
    public String getStrategyName() {
        return "Default Execution Strategy";
    }
    
    @Override
    public int getPriority() {
        return 100; // 默认优先级
    }
    
    /**
     * 创建优化后代码的执行计划
     */
    private ExecutionPlan createOptimizedExecutionPlan(Project project, OptimizationResult result) {
        String optimizedFilePath = project.getWorkDir() + "main_optimized.py";
        
        // Ray优化后的环境变量
        Map<String, String> envVars = Map.of(
            "RAY_TMPDIR", "/tmp/ray",
            "RAY_DISABLE_IMPORT_WARNING", "1",
            "RAY_DISABLE_TELEMETRY", "1",
            "RAY_DEDUP_LOGS", "0"
        );
        
        String description = String.format(
            "执行Ray优化代码 (预期加速%.1fx) - %s", 
            result.getExpectedSpeedup(),
            result.getOptimizerName()
        );
        
        return ExecutionPlan.builder()
                .executableFilePath(optimizedFilePath)
                .command("python " + optimizedFilePath)
                .environmentVariables(envVars)
                .isOptimized(true)
                .description(description)
                .workingDirectory(project.getWorkDir())
                .timeoutSeconds(300)
                .build();
    }
    
    /**
     * 创建原始代码的执行计划
     */
    private ExecutionPlan createOriginalExecutionPlan(Project project, OptimizationResult result) {
        String reason = result.getErrorMessage() != null ? 
            result.getErrorMessage() : "未发现可优化的代码模式";
        
        String description = String.format("执行原始代码 (%s)", reason);
        
        return ExecutionPlan.builder()
                .executableFilePath(project.getPyPath())
                .command("python " + project.getPyPath())
                .environmentVariables(Collections.emptyMap())
                .isOptimized(false)
                .description(description)
                .workingDirectory(project.getWorkDir())
                .timeoutSeconds(300)
                .build();
    }
} 