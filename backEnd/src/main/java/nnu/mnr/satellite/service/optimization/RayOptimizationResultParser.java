package nnu.mnr.satellite.service.optimization;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ray优化结果解析器
 * 遵循SRP原则 - 专注于解析Ray优化的输出结果
 * 
 * @author zzw
 * @date 2025/01/20
 */
@Slf4j
public class RayOptimizationResultParser {
    
    private static final String RESULT_MARKER = "RAY_OPTIMIZATION_RESULT:";
    private static final String ERROR_MARKER = "RAY_OPTIMIZATION_ERROR:";
    private static final Pattern RESULT_PATTERN = Pattern.compile(
        RESULT_MARKER + "\\s*(.+)", Pattern.DOTALL);
    private static final Pattern ERROR_PATTERN = Pattern.compile(
        ERROR_MARKER + "\\s*(.+)", Pattern.DOTALL);
    
    /**
     * 解析Ray优化结果
     * @param originalCode 原始代码
     * @param output 优化命令的输出
     * @return 优化结果
     */
    public static OptimizationResult parse(String originalCode, String output) {
        try {
            if (output == null || output.trim().isEmpty()) {
                return OptimizationResult.failure(originalCode, "No output from optimization", "Ray");
            }
            
            // 先检查是否有错误信息
            String errorMessage = extractErrorMessage(output);
            if (errorMessage != null) {
                return OptimizationResult.failure(originalCode, errorMessage, "Ray");
            }
            
            // 提取结果JSON
            String resultJson = extractResultJson(output);
            if (resultJson == null) {
                return OptimizationResult.failure(originalCode, "No optimization result found in output", "Ray");
            }
            
            // 解析JSON结果
            return parseJsonResult(originalCode, resultJson);
            
        } catch (Exception e) {
            log.error("Failed to parse Ray optimization result: {}", e.getMessage());
            return OptimizationResult.failure(originalCode, 
                "Failed to parse optimization result: " + e.getMessage(), "Ray");
        }
    }
    
    /**
     * 从输出中提取结果JSON
     */
    private static String extractResultJson(String output) {
        Matcher matcher = RESULT_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    /**
     * 从输出中提取错误信息
     */
    private static String extractErrorMessage(String output) {
        Matcher matcher = ERROR_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    /**
     * 解析JSON结果
     */
    private static OptimizationResult parseJsonResult(String originalCode, String jsonStr) {
        try {
            JSONObject result = JSONObject.parseObject(jsonStr);
            
            boolean success = result.getBooleanValue("success");
            if (!success) {
                String error = result.getString("error");
                return OptimizationResult.failure(originalCode, error != null ? error : "Optimization failed", "Ray");
            }
            
            String optimizedCode = result.getString("optimized_code");
            if (optimizedCode == null || optimizedCode.trim().isEmpty()) {
                return OptimizationResult.failure(originalCode, "No optimized code generated", "Ray");
            }
            
            double speedup = result.getDoubleValue("speedup_factor");
            if (speedup <= 0) {
                speedup = 1.0; // 默认无加速
            }
            
            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("analysis_time", result.get("analysis_time"));
            metadata.put("optimization_type", result.get("optimization_type"));
            metadata.put("parallel_opportunities", result.get("parallel_opportunities"));
            metadata.put("estimated_memory_usage", result.get("estimated_memory_usage"));
            
            return OptimizationResult.builder()
                    .successful(true)
                    .originalCode(originalCode)
                    .optimizedCode(optimizedCode)
                    .expectedSpeedup(speedup)
                    .optimizerName("Ray")
                    .metadata(metadata)
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to parse JSON result: {}", e.getMessage());
            return OptimizationResult.failure(originalCode, 
                "JSON parsing failed: " + e.getMessage(), "Ray");
        }
    }
    
    /**
     * 检查输出是否包含错误信息
     */
    public static boolean hasError(String output) {
        if (output == null) return true;
        
        String lowerOutput = output.toLowerCase();
        return lowerOutput.contains("error") || 
               lowerOutput.contains("exception") || 
               lowerOutput.contains("traceback");
    }
    
    /**
     * 提取错误信息
     */
    public static String extractError(String output) {
        if (output == null) return "No output";
        
        // 查找最后一个错误信息
        String[] lines = output.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.toLowerCase().contains("error") || 
                line.toLowerCase().contains("exception")) {
                return line;
            }
        }
        
        return "Unknown error in optimization output";
    }
} 