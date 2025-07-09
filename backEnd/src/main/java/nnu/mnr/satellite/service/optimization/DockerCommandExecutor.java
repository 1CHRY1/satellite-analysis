package nnu.mnr.satellite.service.optimization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.service.common.DockerService;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Docker命令执行器
 * 遵循DRY原则 - 避免重复的Docker命令执行代码
 * 遵循SRP原则 - 专注于Docker命令执行
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Component
@Slf4j
public class DockerCommandExecutor {
    
    private final DockerService dockerService;
    
    public DockerCommandExecutor(DockerService dockerService) {
        this.dockerService = dockerService;
    }
    
    /**
     * 执行命令并获取输出
     * @param containerId 容器ID
     * @param command 命令
     * @param timeout 超时时间
     * @return 命令执行结果
     */
    public CommandResult executeCommand(String containerId, String command, Duration timeout) {
        try {
            log.debug("Executing command in container {}: {}", containerId, command);
            String output = dockerService.runCMDInContainerAndGetOutput(containerId, command);
            return CommandResult.success(output);
        } catch (Exception e) {
            log.error("Docker command execution failed: {}", e.getMessage());
            return CommandResult.failure(e.getMessage());
        }
    }
    
    /**
     * 静默执行命令（不抛出异常）
     * @param containerId 容器ID
     * @param command 命令
     */
    public void executeCommandSilent(String containerId, String command) {
        try {
            log.debug("Executing silent command in container {}: {}", containerId, command);
            dockerService.runCMDInContainerSilent(containerId, command);
        } catch (Exception e) {
            log.debug("Silent command failed (ignored): {}", e.getMessage());
        }
    }
    
    /**
     * 检查命令执行是否成功
     * @param containerId 容器ID
     * @param testCommand 测试命令
     * @return 是否成功
     */
    public boolean testCommand(String containerId, String testCommand) {
        try {
            CommandResult result = executeCommand(containerId, testCommand, Duration.ofSeconds(10));
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 命令执行结果
     */
    @Data
    @AllArgsConstructor
    public static class CommandResult {
        private boolean success;
        private String output;
        private String error;
        
        public static CommandResult success(String output) {
            return new CommandResult(true, output, null);
        }
        
        public static CommandResult failure(String error) {
            return new CommandResult(false, null, error);
        }
        
        public boolean hasOutput() {
            return output != null && !output.trim().isEmpty();
        }
        
        public String getCleanOutput() {
            return output != null ? output.trim() : "";
        }
    }
} 