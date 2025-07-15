package nnu.mnr.satellite.service.common;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/17 12:13
 * @Description:
 */

@Service("BandMapperGenerator")
public class BandMapperGenerator {

    private final JSONObject config;

    public BandMapperGenerator(@Value("${satelliteConfig.path}") String configPath
    ) {
        this.config = loadJsonFromResources(configPath);
    }

    public JSONObject getSatelliteConfigBySensorName(String sensorName) {
        return config.getJSONObject(sensorName);
    }

    /**
     * 从指定路径读取 JSON 文件并加载到 JSONObject
     * @param filePath JSON 文件路径
     * @return JSONObject 对象，如果读取失败则返回 null
     */
    public static JSONObject loadJsonFromResources(String filePath) {
        try {
            // 读取文件内容为字符串
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
            // 将字符串解析为 JSONObject
            return JSONObject.parseObject(jsonString);
        } catch (IOException e) {
            System.err.println("Error reading the JSON file: " + e.getMessage());
            return null;
        }
    }

}
