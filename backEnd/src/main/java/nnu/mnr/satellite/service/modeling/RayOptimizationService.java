package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.dto.modeling.ProjectBasicDTO;
import nnu.mnr.satellite.model.vo.modeling.CodingProjectVO;
import nnu.mnr.satellite.model.po.modeling.Project;
import nnu.mnr.satellite.service.common.SftpDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Ray优化服务
 * 
 * @Author: zzw
 * @Date: 2025/7/2 11:25
 * @Description: 为用户Python代码提供Ray优化功能
 */
@Service
@Slf4j
public class RayOptimizationService {

    @Autowired
    private SftpDataService sftpDataService;

    @Autowired
    private ProjectDataService projectDataService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${modelServer.address:http://localhost:5002}")
    private String modelServerAddress;

    @Value("${ray.optimization.enabled:true}")
    private boolean rayOptimizationEnabled;

    @Value("${ray.optimization.timeout:30}")
    private int optimizationTimeoutSeconds;

    // 优化结果缓存
    private final Map<String, CachedOptimization> optimizationCache = new ConcurrentHashMap<>();
    
    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRY_MS = 10 * 60 * 1000; // 10分钟

    /**
     * 优化用户Python代码
     * 
     * @param projectBasicDTO 项目基本信息
     * @return 优化结果
     */
    public CodingProjectVO optimizeUserCode(ProjectBasicDTO projectBasicDTO) {
        String userId = projectBasicDTO.getUserId();
        String projectId = projectBasicDTO.getProjectId();
        String responseInfo = "";

        if (!rayOptimizationEnabled) {
            return CodingProjectVO.builder()
                    .status(-1)
                    .info("Ray优化功能已禁用")
                    .projectId(projectId)
                    .build();
        }

        try {
            // 验证用户权限
            if (!projectDataService.VerifyUserProject(userId, projectId)) {
                responseInfo = "User " + userId + " Can't Operate Project " + projectId;
                return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
            }

            // 获取项目信息
            Project project = projectDataService.getProjectById(projectId);
            if (project == null) {
                responseInfo = "Project " + projectId + " hasn't been Registered";
                return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
            }

            // 读取原始Python代码
            String originalCode = project.getPyContent();
            if (originalCode == null || originalCode.trim().isEmpty()) {
                responseInfo = "No Python code found in project " + projectId;
                return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
            }

            // 检查缓存
            String cacheKey = generateCacheKey(originalCode, userId, projectId);
            CachedOptimization cached = optimizationCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.info("使用缓存的优化结果，项目ID: {}", projectId);
                return buildSuccessResponse(cached.getOptimizedPath(), cached.getReport(), projectId);
            }

            // 调用Ray优化服务（HTTP API方式）
            RayOptimizationResult optimizationResult = callRayOptimizationHTTP(originalCode, userId, projectId);

            if (optimizationResult.isSuccess()) {
                // 保存优化后的代码
                String optimizedScriptPath = saveOptimizedScript(project, optimizationResult.getOptimizedCode());
                
                // 缓存优化结果
                optimizationCache.put(cacheKey, new CachedOptimization(
                    optimizedScriptPath, 
                    optimizationResult.getReport(),
                    System.currentTimeMillis()
                ));

                return buildSuccessResponse(optimizedScriptPath, optimizationResult.getReport(), projectId);
            } else {
                responseInfo = "Ray optimization failed: " + optimizationResult.getError();
                return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
            }

        } catch (Exception e) {
            log.error("Ray optimization error for project {}: {}", projectId, e.getMessage(), e);
            responseInfo = "Ray optimization failed: " + e.getMessage();
            return CodingProjectVO.builder().status(-1).info(responseInfo).projectId(projectId).build();
        }
    }

    /**
     * 为项目创建Ray优化的执行脚本
     * 
     * @param project 项目对象
     * @param userId 用户ID
     * @return 优化后的脚本路径
     */
    public String createRayOptimizedScript(Project project, String userId) throws Exception {
        String projectId = project.getProjectId();
        String originalCode = project.getPyContent();

        if (originalCode == null || originalCode.trim().isEmpty()) {
            throw new IllegalArgumentException("No Python code found in project");
        }

        // 检查是否启用Ray优化
        if (!rayOptimizationEnabled) {
            log.info("Ray优化已禁用，使用原始脚本，项目ID: {}", projectId);
            return project.getPyPath();
        }

        // 检查缓存
        String cacheKey = generateCacheKey(originalCode, userId, projectId);
        CachedOptimization cached = optimizationCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("使用缓存的优化脚本，项目ID: {}", projectId);
            return cached.getOptimizedPath();
        }

        // 调用Ray优化API
        RayOptimizationResult result = callRayOptimizationHTTP(originalCode, userId, projectId);

        if (result.isSuccess()) {
            // 保存优化脚本
            String optimizedPath = saveOptimizedScript(project, result.getOptimizedCode());
            
            // 缓存结果
            optimizationCache.put(cacheKey, new CachedOptimization(
                optimizedPath, 
                result.getReport(),
                System.currentTimeMillis()
            ));
            
            log.info("Ray优化成功完成，项目ID: {}, 优化脚本路径: {}, 预估加速比: {}x", 
                    projectId, optimizedPath, result.getReport().getEstimatedSpeedup());

            return optimizedPath;
        } else {
            log.warn("Ray优化失败，使用原始脚本，项目ID: {}, 错误: {}", projectId, result.getError());
            return project.getPyPath();
        }
    }

    /**
     * 通过HTTP API调用Ray优化服务
     */
    private RayOptimizationResult callRayOptimizationHTTP(String code, String userId, String projectId) {
        try {
            // 构建请求
            Map<String, Object> requestBody = Map.of(
                "code", code,
                "user_id", userId,
                "project_id", projectId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送HTTP请求到模型服务器
            String url = modelServerAddress + "/v0/ray/optimize";
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    String optimizedCode = (String) responseBody.get("optimized_code");
                    Map<String, Object> reportMap = (Map<String, Object>) responseBody.get("report");
                    
                    OptimizationReport report = OptimizationReport.fromMap(reportMap);
                    return RayOptimizationResult.success(optimizedCode, report);
                } else {
                    String error = (String) responseBody.get("error");
                    return RayOptimizationResult.failure(error);
                }
            } else {
                return RayOptimizationResult.failure("HTTP request failed: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("HTTP Ray optimization API call failed", e);
            // 降级到进程调用方式
            return callRayOptimizationProcess(code, userId, projectId);
        }
    }

    /**
     * 进程调用方式（降级方案）
     */
    private RayOptimizationResult callRayOptimizationProcess(String code, String userId, String projectId) {
        try {
            // 创建临时Python脚本调用优化器
            String optimizerScript = createOptimizerScript(code, userId, projectId);
            
            // 执行优化脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", "-c", optimizerScript
            );
            
            processBuilder.environment().put("PYTHONPATH", 
                System.getProperty("user.dir") + "/modelServer/dataProcessing");
            
            Process process = processBuilder.start();
            
            // 读取输出
            String output = readProcessOutput(process);
            String error = readProcessError(process);
            
            boolean finished = process.waitFor(optimizationTimeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return RayOptimizationResult.failure("Optimization timeout after " + optimizationTimeoutSeconds + " seconds");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode == 0) {
                // 解析优化结果
                return parseOptimizationResult(output);
            } else {
                log.error("Ray optimization process failed with exit code {}: {}", exitCode, error);
                return RayOptimizationResult.failure("Optimization process failed: " + error);
            }
            
        } catch (Exception e) {
            log.error("Error calling Ray optimization API", e);
            return RayOptimizationResult.failure("API call failed: " + e.getMessage());
        }
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String code, String userId, String projectId) {
        return String.format("%s_%s_%d", userId, projectId, code.hashCode());
    }

    /**
     * 构建成功响应
     */
    private CodingProjectVO buildSuccessResponse(String optimizedPath, OptimizationReport report, String projectId) {
        String responseInfo = String.format("Ray优化完成！优化脚本已保存至: %s. " +
                "应用的优化: %s, 预估加速比: %.1fx", 
                optimizedPath,
                String.join(", ", report.getAppliedOptimizations()),
                report.getEstimatedSpeedup());

        return CodingProjectVO.builder()
                .status(1)
                .info(responseInfo)
                .projectId(projectId)
                .build();
    }

    /**
     * 清理过期缓存
     */
    public void cleanExpiredCache() {
        optimizationCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 缓存优化结果
     */
    private static class CachedOptimization {
        private final String optimizedPath;
        private final OptimizationReport report;
        private final long timestamp;

        public CachedOptimization(String optimizedPath, OptimizationReport report, long timestamp) {
            this.optimizedPath = optimizedPath;
            this.report = report;
            this.timestamp = timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }

        public String getOptimizedPath() { return optimizedPath; }
        public OptimizationReport getReport() { return report; }
    }

    /**
     * 创建优化器调用脚本
     */
    private String createOptimizerScript(String code, String userId, String projectId) {
        return String.format("""
            import sys
            import json
            from service.ray_optimizer import optimize_user_code
            
            # 用户代码
            user_code = '''%s'''
            
            try:
                # 执行优化
                optimized_code, report = optimize_user_code(user_code, '%s', '%s')
                
                # 输出结果
                result = {
                    'success': True,
                    'optimized_code': optimized_code,
                    'report': report
                }
                print('RAY_OPTIMIZATION_RESULT:' + json.dumps(result, ensure_ascii=False))
                
            except Exception as e:
                result = {
                    'success': False,
                    'error': str(e)
                }
                print('RAY_OPTIMIZATION_RESULT:' + json.dumps(result, ensure_ascii=False))
            """, 
            code.replace("'''", "\\'\\'\\'"), userId, projectId);
    }

    /**
     * 保存优化后的脚本
     */
    private String saveOptimizedScript(Project project, String optimizedCode) throws Exception {
        String originalPath = project.getServerPyPath();
        String optimizedPath = originalPath.replace(".py", "_ray_optimized.py");
        
        // 通过SFTP保存优化后的代码
        try (InputStream inputStream = new ByteArrayInputStream(optimizedCode.getBytes(StandardCharsets.UTF_8))) {
            sftpDataService.uploadFileFromStream(inputStream, optimizedPath);
        }
        
        return optimizedPath;
    }

    /**
     * 读取进程输出
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * 读取进程错误输出
     */
    private String readProcessError(Process process) throws IOException {
        StringBuilder error = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }
        return error.toString();
    }

    /**
     * 解析优化结果
     */
    private RayOptimizationResult parseOptimizationResult(String output) {
        try {
            // 查找结果标记
            String resultMarker = "RAY_OPTIMIZATION_RESULT:";
            int markerIndex = output.indexOf(resultMarker);
            
            if (markerIndex == -1) {
                return RayOptimizationResult.failure("No optimization result found in output");
            }
            
            String jsonResult = output.substring(markerIndex + resultMarker.length()).trim();
            Map<String, Object> resultMap = JSON.parseObject(jsonResult, Map.class);
            
            if (Boolean.TRUE.equals(resultMap.get("success"))) {
                String optimizedCode = (String) resultMap.get("optimized_code");
                Map<String, Object> reportMap = (Map<String, Object>) resultMap.get("report");
                
                OptimizationReport report = OptimizationReport.fromMap(reportMap);
                
                return RayOptimizationResult.success(optimizedCode, report);
            } else {
                String error = (String) resultMap.get("error");
                return RayOptimizationResult.failure(error);
            }
            
        } catch (Exception e) {
            log.error("Error parsing optimization result: {}", output, e);
            return RayOptimizationResult.failure("Failed to parse optimization result: " + e.getMessage());
        }
    }

    /**
     * Ray优化结果
     */
    public static class RayOptimizationResult {
        private final boolean success;
        private final String optimizedCode;
        private final OptimizationReport report;
        private final String error;

        private RayOptimizationResult(boolean success, String optimizedCode, OptimizationReport report, String error) {
            this.success = success;
            this.optimizedCode = optimizedCode;
            this.report = report;
            this.error = error;
        }

        public static RayOptimizationResult success(String optimizedCode, OptimizationReport report) {
            return new RayOptimizationResult(true, optimizedCode, report, null);
        }

        public static RayOptimizationResult failure(String error) {
            return new RayOptimizationResult(false, null, null, error);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getOptimizedCode() { return optimizedCode; }
        public OptimizationReport getReport() { return report; }
        public String getError() { return error; }
    }

    /**
     * 优化报告
     */
    public static class OptimizationReport {
        private java.util.List<String> appliedOptimizations;
        private double estimatedSpeedup;
        private java.util.List<String> rayFeaturesUsed;
        private java.util.List<String> recommendations;

        public static OptimizationReport fromMap(Map<String, Object> map) {
            OptimizationReport report = new OptimizationReport();
            report.appliedOptimizations = (java.util.List<String>) map.get("applied_optimizations");
            report.estimatedSpeedup = ((Number) map.getOrDefault("estimated_speedup", 1.0)).doubleValue();
            report.rayFeaturesUsed = (java.util.List<String>) map.get("ray_features_used");
            report.recommendations = (java.util.List<String>) map.get("recommendations");
            return report;
        }

        // Getters
        public java.util.List<String> getAppliedOptimizations() { return appliedOptimizations; }
        public double getEstimatedSpeedup() { return estimatedSpeedup; }
        public java.util.List<String> getRayFeaturesUsed() { return rayFeaturesUsed; }
        public java.util.List<String> getRecommendations() { return recommendations; }
    }
} 