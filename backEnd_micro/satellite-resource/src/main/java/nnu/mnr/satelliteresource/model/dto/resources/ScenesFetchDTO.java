package nnu.mnr.satelliteresource.model.dto.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

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
    private String startTime;
    private String endTime;
    private JSONObject geometry;

}
