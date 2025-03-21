package nnu.mnr.satellite.model.po.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 17:22
 * @Description:
 */

@Data
public class ModelResult {

    private String id;
    private JSONObject info;
    private String bucket;
    private String path;

}
