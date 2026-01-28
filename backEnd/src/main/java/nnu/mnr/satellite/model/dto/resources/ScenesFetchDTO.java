package nnu.mnr.satellite.model.dto.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 20:08
 * @Description:
 */

@Data
public class ScenesFetchDTO {

    private String sensorId;
    private String productId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private JSONObject geometry;

}
