package nnu.mnr.satellite.service.optimization;

import lombok.Builder;
import lombok.Data;
import nnu.mnr.satellite.model.po.modeling.Project;

/**
 * 优化请求数据类
 * 遵循SRP原则 - 封装优化请求的所有信息
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Data
@Builder
public class OptimizationRequest {
    
    /**
     * 项目信息
     */
    private Project project;
    
    /**
     * Docker容器ID
     */
    private String containerId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 优化模式
     */
    private OptimizationContext.OptimizationMode mode;
    
    /**
     * 是否强制优化（忽略缓存）
     */
    private boolean forceOptimization;
    
    /**
     * 创建默认的优化请求
     */
    public static OptimizationRequest create(Project project, String containerId, String userId) {
        return OptimizationRequest.builder()
                .project(project)
                .containerId(containerId)
                .userId(userId)
                .mode(OptimizationContext.OptimizationMode.AUTO)
                .forceOptimization(false)
                .build();
    }
} 