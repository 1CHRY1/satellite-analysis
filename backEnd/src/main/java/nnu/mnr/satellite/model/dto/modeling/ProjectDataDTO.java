package nnu.mnr.satellite.model.dto.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/28 20:36
 * @Description:
 */

@Data
public class ProjectDataDTO {

    private String userId;
    private String projectId;
    private String uploadDataName;
    private JSONObject uploadData;

}
