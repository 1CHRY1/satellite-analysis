package nnu.mnr.satellite.model.dto.user;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

@Data
public class RecordSaveDTO {
    private String userId;
    private String actionType;
    private JSONObject actionDetail;
    private String actionTime;
}
