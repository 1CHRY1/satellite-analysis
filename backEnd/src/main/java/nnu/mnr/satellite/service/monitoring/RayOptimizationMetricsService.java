package nnu.mnr.satellite.service.monitoring;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.config.RayOptimizationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Ray优化监控指标服务
 * 
 * @Author: zzw
 * @Date: 2025/7/2 11:25
 * @Description: 收集和管理Ray优化相关指标
 */
@Service
@Slf4j
public class RayOptimizationMetricsService {

    @Autowired
    private RayOptimizationConfig.MonitoringConfig monitoringConfig;

    // 指标数据
    private final AtomicLong totalOptimizations = new AtomicLong(0);
    private final AtomicLong successfulOptimizations = new AtomicLong(0);
    private final AtomicLong failedOptimizations = new AtomicLong(0);
    private final AtomicLong timeoutOptimizations = new AtomicLong(0);
    private final DoubleAdder totalExecutionTime = new DoubleAdder();
    private final DoubleAdder totalSpeedupGain = new DoubleAdder();
    
    // 优化类型统计
    private final ConcurrentHashMap<String, AtomicLong> optimizationTypes = new ConcurrentHashMap<>();
    
    // 用户使用统计
    private final ConcurrentHashMap<String, UserMetrics> userMetrics = new ConcurrentHashMap<>();
    
    // 最近的优化记录
    private final List<OptimizationRecord> recentOptimizations = new ArrayList<>();
    private static final int MAX_RECENT_RECORDS = 100;

    /**
     * 记录优化事件
     */
    public void recordOptimization(OptimizationEvent event) {
        totalOptimizations.incrementAndGet();
        
        if (event.isSuccess()) {
            successfulOptimizations.incrementAndGet();
            totalExecutionTime.add(event.getExecutionTime());
            totalSpeedupGain.add(event.getSpeedupGain());
            
            // 记录优化类型
            for (String optimization : event.getAppliedOptimizations()) {
                optimizationTypes.computeIfAbsent(optimization, k -> new AtomicLong(0))
                                .incrementAndGet();
            }
        } else {
            failedOptimizations.incrementAndGet();
            if (event.isTimeout()) {
                timeoutOptimizations.incrementAndGet();
            }
        }
        
        // 更新用户指标
        updateUserMetrics(event);
        
        // 记录最近的优化
        addRecentOptimization(event);
        
        // 检查告警阈值
        checkAlertThresholds();
    }

    /**
     * 获取整体指标
     */
    public OverallMetrics getOverallMetrics() {
        long total = totalOptimizations.get();
        long successful = successfulOptimizations.get();
        long failed = failedOptimizations.get();
        
        double successRate = total > 0 ? (double) successful / total : 0.0;
        double errorRate = total > 0 ? (double) failed / total : 0.0;
        double averageExecutionTime = successful > 0 ? totalExecutionTime.sum() / successful : 0.0;
        double averageSpeedup = successful > 0 ? totalSpeedupGain.sum() / successful : 1.0;
        
        return OverallMetrics.builder()
                .totalOptimizations(total)
                .successfulOptimizations(successful)
                .failedOptimizations(failed)
                .timeoutOptimizations(timeoutOptimizations.get())
                .successRate(successRate)
                .errorRate(errorRate)
                .averageExecutionTime(averageExecutionTime)
                .averageSpeedup(averageSpeedup)
                .build();
    }

    /**
     * 获取优化类型统计
     */
    public Map<String, Long> getOptimizationTypeStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        optimizationTypes.forEach((type, count) -> stats.put(type, count.get()));
        return stats;
    }

    /**
     * 获取用户指标
     */
    public Map<String, UserMetrics> getUserMetrics() {
        return new ConcurrentHashMap<>(userMetrics);
    }

    /**
     * 获取最近的优化记录
     */
    public List<OptimizationRecord> getRecentOptimizations(int limit) {
        synchronized (recentOptimizations) {
            int size = recentOptimizations.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(recentOptimizations.subList(start, size));
        }
    }

    /**
     * 更新用户指标
     */
    private void updateUserMetrics(OptimizationEvent event) {
        userMetrics.compute(event.getUserId(), (userId, metrics) -> {
            if (metrics == null) {
                metrics = new UserMetrics();
            }
            
            metrics.totalOptimizations++;
            if (event.isSuccess()) {
                metrics.successfulOptimizations++;
                metrics.totalSpeedupGain += event.getSpeedupGain();
            } else {
                metrics.failedOptimizations++;
            }
            
            return metrics;
        });
    }

    /**
     * 添加最近的优化记录
     */
    private void addRecentOptimization(OptimizationEvent event) {
        OptimizationRecord record = OptimizationRecord.builder()
                .userId(event.getUserId())
                .projectId(event.getProjectId())
                .success(event.isSuccess())
                .executionTime(event.getExecutionTime())
                .speedupGain(event.getSpeedupGain())
                .appliedOptimizations(event.getAppliedOptimizations())
                .errorMessage(event.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        synchronized (recentOptimizations) {
            recentOptimizations.add(record);
            if (recentOptimizations.size() > MAX_RECENT_RECORDS) {
                recentOptimizations.remove(0);
            }
        }
    }

    /**
     * 检查告警阈值
     */
    private void checkAlertThresholds() {
        if (!monitoringConfig.isEnabled()) {
            return;
        }
        
        OverallMetrics metrics = getOverallMetrics();
        
        // 检查错误率
        if (metrics.getErrorRate() > monitoringConfig.getAlertThresholds().getErrorRate()) {
            log.warn("Ray优化错误率过高: {:.2f}%, 阈值: {:.2f}%", 
                    metrics.getErrorRate() * 100,
                    monitoringConfig.getAlertThresholds().getErrorRate() * 100);
        }
        
        // 检查超时率
        double timeoutRate = metrics.getTotalOptimizations() > 0 ? 
                (double) metrics.getTimeoutOptimizations() / metrics.getTotalOptimizations() : 0.0;
        
        if (timeoutRate > monitoringConfig.getAlertThresholds().getTimeoutRate()) {
            log.warn("Ray优化超时率过高: {:.2f}%, 阈值: {:.2f}%", 
                    timeoutRate * 100,
                    monitoringConfig.getAlertThresholds().getTimeoutRate() * 100);
        }
    }

    /**
     * 定期输出指标报告
     */
    @Scheduled(fixedRateString = "#{${monitoring.ray.metrics-interval:30} * 1000}")
    public void reportMetrics() {
        if (!monitoringConfig.isEnabled()) {
            return;
        }
        
        OverallMetrics metrics = getOverallMetrics();
        
        log.info("Ray优化指标报告 - 总数: {}, 成功: {}, 失败: {}, 成功率: {:.2f}%, 平均加速: {:.2f}x", 
                metrics.getTotalOptimizations(),
                metrics.getSuccessfulOptimizations(),
                metrics.getFailedOptimizations(),
                metrics.getSuccessRate() * 100,
                metrics.getAverageSpeedup());
    }

    /**
     * 重置指标
     */
    public void resetMetrics() {
        totalOptimizations.set(0);
        successfulOptimizations.set(0);
        failedOptimizations.set(0);
        timeoutOptimizations.set(0);
        totalExecutionTime.reset();
        totalSpeedupGain.reset();
        optimizationTypes.clear();
        userMetrics.clear();
        
        synchronized (recentOptimizations) {
            recentOptimizations.clear();
        }
        
        log.info("Ray优化指标已重置");
    }

    /**
     * 优化事件数据类
     */
    @Data
    public static class OptimizationEvent {
        private String userId;
        private String projectId;
        private boolean success;
        private boolean timeout;
        private double executionTime;
        private double speedupGain;
        private List<String> appliedOptimizations;
        private String errorMessage;
        
        public static OptimizationEvent success(String userId, String projectId, 
                                              double executionTime, double speedupGain,
                                              List<String> appliedOptimizations) {
            OptimizationEvent event = new OptimizationEvent();
            event.userId = userId;
            event.projectId = projectId;
            event.success = true;
            event.timeout = false;
            event.executionTime = executionTime;
            event.speedupGain = speedupGain;
            event.appliedOptimizations = appliedOptimizations;
            return event;
        }
        
        public static OptimizationEvent failure(String userId, String projectId, 
                                              String errorMessage, boolean timeout) {
            OptimizationEvent event = new OptimizationEvent();
            event.userId = userId;
            event.projectId = projectId;
            event.success = false;
            event.timeout = timeout;
            event.errorMessage = errorMessage;
            event.appliedOptimizations = new ArrayList<>();
            return event;
        }
    }

    /**
     * 整体指标数据类
     */
    @Data
    @lombok.Builder
    public static class OverallMetrics {
        private long totalOptimizations;
        private long successfulOptimizations;
        private long failedOptimizations;
        private long timeoutOptimizations;
        private double successRate;
        private double errorRate;
        private double averageExecutionTime;
        private double averageSpeedup;
    }

    /**
     * 用户指标数据类
     */
    @Data
    public static class UserMetrics {
        private long totalOptimizations = 0;
        private long successfulOptimizations = 0;
        private long failedOptimizations = 0;
        private double totalSpeedupGain = 0.0;
        
        public double getAverageSpeedup() {
            return successfulOptimizations > 0 ? totalSpeedupGain / successfulOptimizations : 1.0;
        }
        
        public double getSuccessRate() {
            return totalOptimizations > 0 ? (double) successfulOptimizations / totalOptimizations : 0.0;
        }
    }

    /**
     * 优化记录数据类
     */
    @Data
    @lombok.Builder
    public static class OptimizationRecord {
        private String userId;
        private String projectId;
        private boolean success;
        private double executionTime;
        private double speedupGain;
        private List<String> appliedOptimizations;
        private String errorMessage;
        private LocalDateTime timestamp;
    }
} 