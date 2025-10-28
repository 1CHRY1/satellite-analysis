package nnu.mnr.satellite.model.dto.admin.scene;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;

@Data
public class SceneUpdateDTO {
    private String sceneId;
    private String sensorId;
    private String productId;
    private String sceneName;
    private LocalDateTime sceneTime;
    private String coordinateSystem;
    private String description;
    private HashSet<String> bands;
    private Integer bandNum;
    private float cloud;
    private JSONObject tags;
    private Double noData;
}
