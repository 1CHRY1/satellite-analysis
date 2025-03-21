package nnu.mnr.satellite.model.dto.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import nnu.mnr.satellite.model.po.resources.Scene;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 21:24
 * @Description:
 */

@Data
public class NdviDTO {

    private String sensorName;
    private List<String> scenes;
    private JSONObject geometry;

}
