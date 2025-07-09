package nnu.mnr.satellite.service.optimization;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.optimization.RayOptimizationProperties;
import nnu.mnr.satellite.service.common.SftpDataService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Ray执行环境管理器
 * 遵循SRP原则 - 专注于Ray环境的准备和清理
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Component
@Slf4j
public class RayExecutionEnvironmentManager implements ExecutionEnvironmentManager {
    
    private final DockerCommandExecutor dockerExecutor;
    private final SftpDataService sftpDataService;
    private final RayOptimizationProperties properties;
    
    public RayExecutionEnvironmentManager(DockerCommandExecutor dockerExecutor,
                                         SftpDataService sftpDataService,
                                         RayOptimizationProperties properties) {
        this.dockerExecutor = dockerExecutor;
        this.sftpDataService = sftpDataService;
        this.properties = properties;
    }
    
    @Override
    public void prepareEnvironment(String containerId, OptimizationContext context) {
        try {
            log.info("Preparing Ray environment for container: {}", containerId);
            
            // 1. 确保Ray优化模块可用
            ensureRayModule(containerId);
            
            // 2. 确保Ray依赖已安装
            ensureRayDependencies(containerId);
            
            // 3. 设置Ray环境变量
            setupRayEnvironment(containerId);
            
            // 4. 创建必要的目录
            createRequiredDirectories(containerId);
            
            log.info("Ray environment prepared successfully for container: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to prepare Ray environment for container {}: {}", containerId, e.getMessage());
            throw new EnvironmentPreparationException("Ray environment preparation failed", e);
        }
    }
    
    @Override
    public void cleanupEnvironment(String containerId, OptimizationContext context) {
        try {
            log.debug("Cleaning up Ray environment for container: {}", containerId);
            
            // 1. 清理临时文件
            cleanupTempFiles(containerId);
            
            // 2. 停止Ray进程（如果有运行的）
            stopRayProcesses(containerId);
            
            log.debug("Ray environment cleaned up for container: {}", containerId);
        } catch (Exception e) {
            log.warn("Ray environment cleanup failed for container {}: {}", containerId, e.getMessage());
        }
    }
    
    @Override
    public boolean isEnvironmentReady(String containerId, OptimizationContext context) {
        try {
            // 检查Ray模块是否存在
            boolean rayModuleExists = dockerExecutor.testCommand(containerId, 
                "test -d /usr/local/coding/ray_optimization");
            
            // 检查Ray是否已安装
            boolean rayInstalled = dockerExecutor.testCommand(containerId, 
                "python -c \"import ray; print('Ray available')\"");
            
            return rayModuleExists && rayInstalled;
        } catch (Exception e) {
            log.debug("Environment readiness check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 确保Ray优化模块在容器中可用
     */
    private void ensureRayModule(String containerId) {
        // 检查模块是否已存在
        if (dockerExecutor.testCommand(containerId, "test -d /usr/local/coding/ray_optimization")) {
            log.debug("Ray optimization module already exists in container");
            return;
        }
        
        // 复制模块到容器
        String copyCommand = "if [ -d /home/satellite/ray_optimization ]; then " +
                           "cp -r /home/satellite/ray_optimization /usr/local/coding/; " +
                           "echo 'Ray module copied'; " +
                           "else echo 'Ray module source not found'; fi";
        
        dockerExecutor.executeCommandSilent(containerId, copyCommand);
        
        // 设置权限
        dockerExecutor.executeCommandSilent(containerId, "chmod -R 755 /usr/local/coding/ray_optimization");
    }
    
    /**
     * 确保Ray依赖已安装
     */
    private void ensureRayDependencies(String containerId) {
        // 检查Ray是否已安装
        if (dockerExecutor.testCommand(containerId, "python -c \"import ray\"")) {
            log.debug("Ray is already installed in container");
            return;
        }
        
        // 安装Ray
        String installCommand = "pip install ray --quiet || pip3 install ray --quiet";
        dockerExecutor.executeCommandSilent(containerId, installCommand);
        
        // 验证安装
        if (!dockerExecutor.testCommand(containerId, "python -c \"import ray\"")) {
            log.warn("Ray installation verification failed");
        }
    }
    
    /**
     * 设置Ray环境变量
     */
    private void setupRayEnvironment(String containerId) {
        Map<String, String> envVars = Map.of(
            "RAY_TMPDIR", properties.getTempDir(),
            "RAY_DISABLE_IMPORT_WARNING", "1",
            "RAY_DEDUP_LOGS", "0",
            "RAY_DISABLE_TELEMETRY", "1"
        );
        
        envVars.forEach((key, value) -> {
            String command = String.format("export %s=%s", key, value);
            dockerExecutor.executeCommandSilent(containerId, command);
        });
    }
    
    /**
     * 创建必要的目录
     */
    private void createRequiredDirectories(String containerId) {
        String[] directories = {
            properties.getTempDir(),
            "/usr/local/coding/ray_optimization/temp",
            "/usr/local/coding/ray_optimization/cache"
        };
        
        for (String dir : directories) {
            String command = String.format("mkdir -p %s && chmod 777 %s", dir, dir);
            dockerExecutor.executeCommandSilent(containerId, command);
        }
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(String containerId) {
        String[] cleanupPaths = {
            properties.getTempDir() + "/*",
            "/usr/local/coding/ray_optimization/temp/*",
            "/usr/local/coding/ray_optimization/cache/*"
        };
        
        for (String path : cleanupPaths) {
            String command = String.format("rm -rf %s 2>/dev/null || true", path);
            dockerExecutor.executeCommandSilent(containerId, command);
        }
    }
    
    /**
     * 停止Ray进程
     */
    private void stopRayProcesses(String containerId) {
        String stopCommand = "python -c \"" +
                           "try: " +
                           "  import ray; " +
                           "  if ray.is_initialized(): " +
                           "    ray.shutdown(); " +
                           "    print('Ray shutdown'); " +
                           "except: " +
                           "  pass" +
                           "\" 2>/dev/null || true";
        
        dockerExecutor.executeCommandSilent(containerId, stopCommand);
    }
    
    /**
     * 环境准备异常
     */
    public static class EnvironmentPreparationException extends RuntimeException {
        public EnvironmentPreparationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 