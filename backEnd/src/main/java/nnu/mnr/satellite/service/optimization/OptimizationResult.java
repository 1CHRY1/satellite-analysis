package nnu.mnr.satellite.service.optimization;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 优化结果封装 - 遵循SRP原则，封装优化执行的结果信息
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Data
@Builder
public class OptimizationResult {
    
    /**
     * 优化是否成功
     */
    private boolean successful;
    
    /**
     * 优化后的代码
     */
    private String optimizedCode;
    
    /**
     * 原始代码
     */
    private String originalCode;
    
    /**
     * 预期加速比
     */
    private double expectedSpeedup;
    
    /**
     * 优化元数据信息
     */
    private Map<String, Object> metadata;
    
    /**
     * 优化步骤描述
     */
    private List<String> optimizationSteps;
    
    /**
     * 错误信息（如果优化失败）
     */
    private String errorMessage;
    
    /**
     * 优化器名称
     */
    private String optimizerName;
    
    /**
     * 优化耗时（毫秒）
     */
    private long executionTimeMs;
    
    /**
     * 获取可执行的代码
     * @return 如果优化成功返回优化后的代码，否则返回原始代码
     */
    public String getExecutableCode() {
        return successful ? optimizedCode : originalCode;
    }
    
    /**
     * 检查是否有优化
     * @return 是否成功优化
     */
    public boolean hasOptimization() {
        return successful && optimizedCode != null && !optimizedCode.trim().isEmpty();
    }
    
    /**
     * 创建成功的优化结果
     */
    public static OptimizationResult success(String originalCode, String optimizedCode, 
                                           double speedup, String optimizerName) {
        return OptimizationResult.builder()
                .successful(true)
                .originalCode(originalCode)
                .optimizedCode(optimizedCode)
                .expectedSpeedup(speedup)
                .optimizerName(optimizerName)
                .build();
    }
    
    /**
     * 创建失败的优化结果
     */
    public static OptimizationResult failure(String originalCode, String errorMessage, String optimizerName) {
        return OptimizationResult.builder()
                .successful(false)
                .originalCode(originalCode)
                .errorMessage(errorMessage)
                .optimizerName(optimizerName)
                .expectedSpeedup(1.0)
                .build();
    }
    
    /**
     * 创建跳过的优化结果
     */
    public static OptimizationResult skip(String originalCode, String reason) {
        return OptimizationResult.builder()
                .successful(false)
                .originalCode(originalCode)
                .errorMessage(reason)
                .optimizerName("None")
                .expectedSpeedup(1.0)
                .build();
    }
} 