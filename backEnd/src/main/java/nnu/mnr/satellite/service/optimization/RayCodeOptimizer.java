package nnu.mnr.satellite.service.optimization;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.optimization.RayOptimizationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;

/**
 * Ray代码优化器实现
 * 遵循SRP原则 - 专注于Ray优化逻辑
 * 遵循OCP原则 - 可扩展其他优化算法
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Component("rayOptimizer")
@Slf4j
public class RayCodeOptimizer implements CodeOptimizer {
    
    private final RayOptimizationProperties properties;
    private final DockerCommandExecutor dockerExecutor;
    
    public RayCodeOptimizer(RayOptimizationProperties properties, 
                           DockerCommandExecutor dockerExecutor) {
        this.properties = properties;
        this.dockerExecutor = dockerExecutor;
    }
    
    @Override
    public OptimizationResult optimize(OptimizationContext context) {
        if (!supports(context)) {
            return OptimizationResult.skip(context.getOriginalCode(), 
                "Ray optimization not supported for this context");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Starting Ray optimization for project: {}", context.getProjectId());
            
            // 执行优化
            OptimizationResult result = performOptimization(context);
            
            // 设置执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            
            log.info("Ray optimization completed for project {} in {}ms, success: {}", 
                    context.getProjectId(), executionTime, result.isSuccessful());
            
            return result;
            
        } catch (Exception e) {
            log.error("Ray optimization failed for project {}: {}", context.getProjectId(), e.getMessage());
            return OptimizationResult.failure(context.getOriginalCode(), e.getMessage(), getName());
        }
    }
    
    @Override
    public boolean supports(OptimizationContext context) {
        if (!properties.isEnabled()) {
            log.debug("Ray optimization is disabled");
            return false;
        }
        
        if (!properties.isValid()) {
            log.warn("Ray optimization configuration is invalid");
            return false;
        }
        
        if (context.getOriginalCode() == null || context.getOriginalCode().trim().isEmpty()) {
            log.debug("No code to optimize");
            return false;
        }
        
        // 检查代码是否适合Ray优化（包含循环、数组操作等）
        return isCodeSuitableForRayOptimization(context.getOriginalCode());
    }
    
    @Override
    public String getName() {
        return "Ray Parallel Optimizer";
    }
    
    @Override
    public int getPriority() {
        return 10; // 高优先级
    }
    
    /**
     * 执行Ray优化
     */
    private OptimizationResult performOptimization(OptimizationContext context) {
        // 1. 准备代码文件
        prepareCodeFile(context);
        
        // 2. 检查容器内文件状态
        checkContainerFiles(context);
        
        // 3. 构建优化命令
        String optimizationCommand = buildOptimizationCommand(context);
        
        // 4. 执行优化
        DockerCommandExecutor.CommandResult result = dockerExecutor.executeCommand(
            context.getContainerId(), 
            optimizationCommand,
            Duration.ofSeconds(properties.getTimeout())
        );
        
        // 5. 解析结果
        log.info("Ray optimization command output for project {}: {}", context.getProjectId(), result.getOutput());
        log.info("Ray optimization command error for project {}: {}", context.getProjectId(), result.getError());
        return parseOptimizationResult(context, result);
    }
    
    /**
     * 准备代码文件
     */
    private void prepareCodeFile(OptimizationContext context) {
        // 将代码内容写入临时文件，避免命令行长度限制
        String encodedCode = Base64.getEncoder().encodeToString(
            context.getOriginalCode().getBytes());
        
        String writeCommand = String.format(
            // "echo '%s' | base64 -d > /usr/local/coding/temp_code.py",
            "echo '%s' | base64 -d > /workspace/temp_code.py",
            encodedCode
        );
        
        dockerExecutor.executeCommandSilent(context.getContainerId(), writeCommand);
    }
    
    /**
     * 构建优化命令
     */
    private String buildOptimizationCommand(OptimizationContext context) {
        // 创建优化脚本内容
        String optimizationScript = String.format(
            "#!/usr/bin/env python\n" +
            "import sys\n" +
            //"sys.path.append('/usr/local/coding')\n" +
            "sys.path.append('/workspace')\n" +
            "from ray_optimization import optimize_code\n" +
            "import json\n" +
            "\n" +
            "try:\n" +
            "    with open('temp_code.py', 'r') as f:\n" +
            "        code = f.read()\n" +
            "    \n" +
            "    result = optimize_code(\n" +
            "        code_content=code,\n" +
            "        cpu_count=%d,\n" +
            "        memory_mb=%d,\n" +
            "        analysis_mode='%s',\n" +
            "        timeout=%d\n" +
            "    )\n" +
            "    \n" +
            "    print('RAY_OPTIMIZATION_RESULT:', json.dumps(result))\n" +
            "except Exception as e:\n" +
            "    print('RAY_OPTIMIZATION_ERROR:', str(e))\n" +
            "    import traceback\n" +
            "    traceback.print_exc()\n",
            properties.getCpuCount(),
            properties.getMemoryMB(),
            properties.getAnalysisMode(),
            properties.getTimeout()
        );
        
        // 创建脚本文件
        String encodedScript = Base64.getEncoder().encodeToString(optimizationScript.getBytes());
        String createScriptCommand = String.format(
            //"echo '%s' | base64 -d > /usr/local/coding/ray_optimize.py",
            "echo '%s' | base64 -d > /workspace/ray_optimize.py",
            encodedScript
        );
        
        dockerExecutor.executeCommandSilent(context.getContainerId(), createScriptCommand);
        
        // 返回执行脚本的命令
        // return "cd /usr/local/coding && python ray_optimize.py";
        return "cd /workspace && python ray_optimize.py";
    }
    
    /**
     * 解析优化结果
     */
    private OptimizationResult parseOptimizationResult(OptimizationContext context, 
                                                      DockerCommandExecutor.CommandResult result) {
        if (!result.isSuccess()) {
            String error = result.getError() != null ? result.getError() : "Optimization command failed";
            return OptimizationResult.failure(context.getOriginalCode(), error, getName());
        }
        
        // 检查输出是否包含错误
        if (RayOptimizationResultParser.hasError(result.getOutput())) {
            String error = RayOptimizationResultParser.extractError(result.getOutput());
            log.warn("Ray optimization failed with error for project {}: {}", context.getProjectId(), error);
            return OptimizationResult.failure(context.getOriginalCode(), error, getName());
        }
        
        // 解析优化结果
        return RayOptimizationResultParser.parse(context.getOriginalCode(), result.getOutput());
    }
    
    /**
     * 检查容器内文件状态
     */
    private void checkContainerFiles(OptimizationContext context) {
        // 检查temp_code.py是否存在
        DockerCommandExecutor.CommandResult checkCode = dockerExecutor.executeCommand(
            context.getContainerId(), 
            //"ls -la /usr/local/coding/temp_code.py", 
            "ls -la /workspace/temp_code.py", 
            Duration.ofSeconds(10)
        );
        log.info("Check temp_code.py for project {}: {}", context.getProjectId(), checkCode.getOutput());
        
        // 检查ray_optimization模块是否存在
        DockerCommandExecutor.CommandResult checkRayModule = dockerExecutor.executeCommand(
            context.getContainerId(), 
            //"ls -la /usr/local/coding/ray_optimization/", 
            "ls -la /workspace/ray_optimization/", 
            Duration.ofSeconds(10)
        );
        log.info("Check ray_optimization module for project {}: {}", context.getProjectId(), checkRayModule.getOutput());
        
        // 检查Python路径和模块导入
        DockerCommandExecutor.CommandResult checkPythonPath = dockerExecutor.executeCommand(
            context.getContainerId(), 
            // "python -c \"import sys; print('Python path:', sys.path)\"", 
            "cd /workspace && python -c \"import sys; sys.path.append('/workspace'); print('Python path:', sys.path); import ray_optimization; print('Ray module found:', ray_optimization.__file__)\"", 
            Duration.ofSeconds(10)
        );
        // log.info("Check Python path for project {}: {}", context.getProjectId(), checkPythonPath.getOutput());
        log.info("Check Python path and Ray module for project {}: {}", context.getProjectId(), checkPythonPath.getOutput());
    }
    
    /**
     * 检查代码是否适合Ray优化
     */
    private boolean isCodeSuitableForRayOptimization(String code) {
        if (code == null) return false;
        
        String lowerCode = code.toLowerCase();
        
        // 检查是否包含适合并行化的模式
        boolean hasLoops = lowerCode.contains("for ") || lowerCode.contains("while ");
        boolean hasArrayOps = lowerCode.contains("numpy") || lowerCode.contains("pandas") || 
                             lowerCode.contains("list(") || lowerCode.contains("[");
        boolean hasComputeIntensive = lowerCode.contains("range(") || 
                                     lowerCode.contains("map(") || 
                                     lowerCode.contains("filter(");
        
        // 至少包含一种可优化的模式
        return hasLoops || hasArrayOps || hasComputeIntensive;
    }
} 