package nnu.mnr.satellite.service.optimization;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 执行计划 - 封装代码执行的具体方案
 * 遵循SRP原则，专注于执行计划信息的封装
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Data
@Builder
public class ExecutionPlan {
    
    /**
     * 可执行文件路径
     */
    private String executableFilePath;
    
    /**
     * 执行命令
     */
    private String command;
    
    /**
     * 环境变量
     */
    private Map<String, String> environmentVariables;
    
    /**
     * 是否为优化后的代码
     */
    private boolean isOptimized;
    
    /**
     * 执行描述信息
     */
    private String description;
    
    /**
     * 工作目录
     */
    private String workingDirectory;
    
    /**
     * 执行超时时间（秒）
     */
    private int timeoutSeconds;
    
    /**
     * 创建原始代码执行计划
     */
    public static ExecutionPlan forOriginalCode(String filePath, String command, String description) {
        return ExecutionPlan.builder()
                .executableFilePath(filePath)
                .command(command)
                .isOptimized(false)
                .description(description)
                .timeoutSeconds(300) // 默认5分钟超时
                .build();
    }
    
    /**
     * 创建优化后代码执行计划
     */
    public static ExecutionPlan forOptimizedCode(String filePath, String command, 
                                               String description, Map<String, String> envVars) {
        return ExecutionPlan.builder()
                .executableFilePath(filePath)
                .command(command)
                .environmentVariables(envVars)
                .isOptimized(true)
                .description(description)
                .timeoutSeconds(300) // 默认5分钟超时
                .build();
    }
} 