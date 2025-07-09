package nnu.mnr.satellite.service.optimization;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.optimization.RayOptimizationProperties;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 代码优化服务门面
 * 遵循Facade Pattern - 简化复杂子系统的接口
 * 遵循DIP原则 - 依赖抽象而非具体实现
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Service
@Slf4j
public class CodeOptimizationFacade {
    
    private final List<CodeOptimizer> optimizers;
    private final ExecutionEnvironmentManager environmentManager;
    private final CodeExecutionStrategy executionStrategy;
    private final OptimizedCodePersistenceService persistenceService;
    private final RayOptimizationProperties properties;
    
    public CodeOptimizationFacade(List<CodeOptimizer> optimizers,
                                 ExecutionEnvironmentManager environmentManager,
                                 CodeExecutionStrategy executionStrategy,
                                 OptimizedCodePersistenceService persistenceService,
                                 RayOptimizationProperties properties) {
        this.optimizers = optimizers;
        this.environmentManager = environmentManager;
        this.executionStrategy = executionStrategy;
        this.persistenceService = persistenceService;
        this.properties = properties;
        
        // 按优先级排序优化器
        this.optimizers.sort(Comparator.comparingInt(CodeOptimizer::getPriority));
        
        log.info("Initialized CodeOptimizationFacade with {} optimizers", optimizers.size());
        optimizers.forEach(optimizer -> 
            log.debug("Registered optimizer: {} (priority: {})", 
                     optimizer.getName(), optimizer.getPriority()));
    }
    
    /**
     * 执行完整的代码优化流程
     * @param request 优化请求
     * @return 优化结果
     */
    public CodeOptimizationOutcome optimizeAndPrepareExecution(OptimizationRequest request) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting code optimization for project: {}", request.getProject().getProjectId());
        
        OptimizationContext context = createOptimizationContext(request);
        
        try {
            // 1. 准备环境
            prepareEnvironment(request.getContainerId(), context);
            
            // 2. 执行优化
            OptimizationResult result = performOptimization(context);
            
            // 3. 持久化优化后的代码
            if (result.hasOptimization()) {
                persistOptimizedCode(request.getProject(), result.getOptimizedCode());
            }
            
            // 4. 创建执行计划
            ExecutionPlan plan = createExecutionPlan(request.getProject(), result);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Code optimization completed for project {} in {}ms, using optimized code: {}", 
                    request.getProject().getProjectId(), processingTime, result.hasOptimization());
            
            return CodeOptimizationOutcome.success(result, plan, processingTime);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Code optimization failed for project {}: {}", 
                     request.getProject().getProjectId(), e.getMessage());
            
            return createFallbackOutcome(request, e, processingTime);
        } finally {
            // 清理环境
            cleanupEnvironment(request.getContainerId(), context);
        }
    }
    
    /**
     * 准备优化环境
     */
    private void prepareEnvironment(String containerId, OptimizationContext context) {
        try {
            log.debug("Preparing optimization environment for container: {}", containerId);
            environmentManager.prepareEnvironment(containerId, context);
        } catch (Exception e) {
            log.error("Failed to prepare optimization environment: {}", e.getMessage());
            if (!properties.isFallbackOnError()) {
                throw e;
            }
        }
    }
    
    /**
     * 执行优化
     */
    private OptimizationResult performOptimization(OptimizationContext context) {
        // 按优先级尝试每个优化器
        for (CodeOptimizer optimizer : optimizers) {
            if (optimizer.supports(context)) {
                log.info("Using optimizer: {} for project: {}", 
                        optimizer.getName(), context.getProjectId());
                
                try {
                    return optimizer.optimize(context);
                } catch (Exception e) {
                    log.warn("Optimizer {} failed: {}", optimizer.getName(), e.getMessage());
                    
                    if (!properties.isFallbackOnError()) {
                        throw e;
                    }
                    // 继续尝试下一个优化器
                }
            }
        }
        
        // 没有找到合适的优化器或所有优化器都失败
        return OptimizationResult.skip(context.getOriginalCode(), "No suitable optimizer found");
    }
    
    /**
     * 持久化优化后的代码
     */
    private void persistOptimizedCode(nnu.mnr.satellite.model.po.modeling.Project project, String optimizedCode) {
        try {
            persistenceService.saveOptimizedCode(project, optimizedCode);
        } catch (Exception e) {
            log.error("Failed to persist optimized code for project {}: {}", 
                     project.getProjectId(), e.getMessage());
            if (!properties.isFallbackOnError()) {
                throw e;
            }
        }
    }
    
    /**
     * 创建执行计划
     */
    private ExecutionPlan createExecutionPlan(nnu.mnr.satellite.model.po.modeling.Project project, 
                                            OptimizationResult result) {
        return executionStrategy.createExecutionPlan(project, result);
    }
    
    /**
     * 清理环境
     */
    private void cleanupEnvironment(String containerId, OptimizationContext context) {
        try {
            log.debug("Cleaning up optimization environment for container: {}", containerId);
            environmentManager.cleanupEnvironment(containerId, context);
        } catch (Exception e) {
            log.warn("Environment cleanup failed: {}", e.getMessage());
        }
    }
    
    /**
     * 创建优化上下文
     */
    private OptimizationContext createOptimizationContext(OptimizationRequest request) {
        return OptimizationContext.builder()
                .projectId(request.getProject().getProjectId())
                .originalCode(request.getProject().getPyContent())
                .containerId(request.getContainerId())
                .userId(request.getUserId())
                .mode(request.getMode() != null ? request.getMode() : OptimizationContext.OptimizationMode.AUTO)
                .configuration(Collections.emptyMap())
                .build();
    }
    
    /**
     * 创建回退结果
     */
    private CodeOptimizationOutcome createFallbackOutcome(OptimizationRequest request, 
                                                         Exception e, 
                                                         long processingTime) {
        // 创建回退的优化结果
        OptimizationResult fallbackResult = OptimizationResult.failure(
            request.getProject().getPyContent(), 
            e.getMessage(), 
            "System"
        );
        
        // 创建回退的执行计划
        ExecutionPlan fallbackPlan = executionStrategy.createExecutionPlan(
            request.getProject(), fallbackResult);
        
        return CodeOptimizationOutcome.failure(
            e.getMessage(), 
            fallbackResult, 
            fallbackPlan, 
            processingTime
        );
    }
} 