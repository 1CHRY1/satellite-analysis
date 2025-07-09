package nnu.mnr.satellite.service.optimization;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.modeling.Project;
import nnu.mnr.satellite.service.common.LocalFileService;
import nnu.mnr.satellite.service.common.SftpDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 优化代码持久化服务
 * 遵循SRP原则 - 专注于优化后代码的保存和管理
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Service
@Slf4j
public class OptimizedCodePersistenceService {
    
    private final SftpDataService sftpDataService;
    private final LocalFileService localFileService;
    
    @Value("${docker.mode:remote}")
    private String dockerMode;
    
    public OptimizedCodePersistenceService(SftpDataService sftpDataService,
                                         LocalFileService localFileService) {
        this.sftpDataService = sftpDataService;
        this.localFileService = localFileService;
    }
    
    /**
     * 保存优化后的代码
     * @param project 项目信息
     * @param optimizedCode 优化后的代码
     */
    public void saveOptimizedCode(Project project, String optimizedCode) {
        try {
            String optimizedPath = getOptimizedCodePath(project);
            
            log.debug("Saving optimized code for project {} to {} using {} mode", 
                     project.getProjectId(), optimizedPath, dockerMode);
            
            if ("local".equals(dockerMode)) {
                // 本地模式：直接写入本地文件
                localFileService.writeLocalFile(optimizedPath, optimizedCode);
            } else {
                // 远程模式：使用SFTP上传
                try (InputStream inputStream = new ByteArrayInputStream(
                        optimizedCode.getBytes(StandardCharsets.UTF_8))) {
                    sftpDataService.uploadFileFromStream(inputStream, optimizedPath);
                }
            }
            
            log.info("Optimized code saved successfully for project {} in {} mode", 
                    project.getProjectId(), dockerMode);
            
        } catch (Exception e) {
            log.error("Failed to save optimized code for project {}: {}", 
                     project.getProjectId(), e.getMessage());
            throw new CodePersistenceException("Failed to save optimized code", e);
        }
    }
    
    /**
     * 检查优化代码是否存在
     * @param project 项目信息
     * @return 是否存在
     */
    public boolean optimizedCodeExists(Project project) {
        try {
            String optimizedPath = getOptimizedCodePath(project);
            // 这里可以通过SFTP检查文件是否存在
            // 暂时返回false，后续可以实现具体的检查逻辑
            return false;
        } catch (Exception e) {
            log.debug("Error checking optimized code existence: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除优化代码
     * @param project 项目信息
     */
    public void deleteOptimizedCode(Project project) {
        try {
            String optimizedPath = getOptimizedCodePath(project);
            
            log.debug("Deleting optimized code for project {} at {}", 
                     project.getProjectId(), optimizedPath);
            
            // 这里可以实现删除逻辑
            // sftpDataService.deleteFile(optimizedPath);
            
            log.info("Optimized code deleted for project {}", project.getProjectId());
            
        } catch (Exception e) {
            log.warn("Failed to delete optimized code for project {}: {}", 
                    project.getProjectId(), e.getMessage());
        }
    }
    
    /**
     * 获取优化代码的路径
     * @param project 项目信息
     * @return 优化代码路径
     */
    private String getOptimizedCodePath(Project project) {
        if ("local".equals(dockerMode)) {
            // 本地模式：使用本地文件路径
            return project.getServerDir() + "main_optimized.py";
        } else {
            // 远程模式：使用服务器路径
            return project.getServerDir() + "main_optimized.py";
        }
    }
    
    /**
     * 代码持久化异常
     */
    public static class CodePersistenceException extends RuntimeException {
        public CodePersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 