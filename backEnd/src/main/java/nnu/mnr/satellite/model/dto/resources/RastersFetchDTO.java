package nnu.mnr.satellite.model.dto.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/21 20:53
 * @Description:
 */

@Data
public class RastersFetchDTO {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer regionId;
    private String dataType;

}
