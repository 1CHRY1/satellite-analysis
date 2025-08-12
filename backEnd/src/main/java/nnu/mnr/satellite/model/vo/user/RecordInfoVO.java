package nnu.mnr.satellite.model.vo.user;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RecordInfoVO {
    private Integer actionId;
    private String actionType;
    private JSONObject actionDetail;
    private LocalDateTime actionTime;
}
