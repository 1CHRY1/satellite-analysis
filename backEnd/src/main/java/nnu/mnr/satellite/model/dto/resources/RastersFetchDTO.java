package nnu.mnr.satellite.model.dto.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/21 20:53
 * @Description:
 */

@Data
public class RastersFetchDTO {

    private String startTime;
    private String endTime;
    private Integer regionId;
    private String dataType;

}
