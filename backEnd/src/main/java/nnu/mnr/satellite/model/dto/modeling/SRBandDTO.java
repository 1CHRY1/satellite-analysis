package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;
import com.alibaba.fastjson2.JSONObject;

@Data
public class SRBandDTO {
    private Integer columnId;
    private Integer rowId;
    private Integer resolution;
    private JSONObject band;
}
