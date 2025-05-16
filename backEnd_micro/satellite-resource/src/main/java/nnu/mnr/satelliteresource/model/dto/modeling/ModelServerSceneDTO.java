package nnu.mnr.satelliteresource.model.dto.modeling;

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
    private String bucket;
    private LocalDateTime sceneTime;
    private String resolution;
    private List<ModelServerImageDTO> images;

    // 关联表字段
    private String sensorName;
    private String productName;

}
