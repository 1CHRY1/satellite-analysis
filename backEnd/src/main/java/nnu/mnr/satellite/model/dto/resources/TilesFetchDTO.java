package nnu.mnr.satellite.model.dto.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/26 21:01
 * @Description:
 */

@Data
public class TilesFetchDTO {

    private String sceneId;
    private String tileLevel;
    private JSONObject geometry;

}
