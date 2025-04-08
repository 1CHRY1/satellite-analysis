package nnu.mnr.satellite.model.vo.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/8 11:24
 * @Description:
 */

@Data
@Builder(builderMethodName = "jsonResultBuilder")
public class JsonResultVO {

    private String type;
    private JSONObject data;

}
