package nnu.mnr.satellite.model.dto.modeling;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/9 22:38
 * @Description:
 */

@Data
@Builder
public class ModelServerSceneDTO {

    private String sceneId;
    private String cloudPath;
    private Integer cloud;
    private String bucket;
    private LocalDateTime sceneTime;
    private String resolution;
    private List<ModelServerImageDTO> images;
    private JSONObject bbox;

    // 关联表字段
    private String sensorName;
    private String productName;

    // 波段配置
    private JSONObject bandMapper;

}
