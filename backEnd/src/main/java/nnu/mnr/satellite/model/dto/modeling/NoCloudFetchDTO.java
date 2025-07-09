package nnu.mnr.satellite.model.dto.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/9 16:38
 * @Description:
 */

@Data
@Builder
public class NoCloudFetchDTO {

    private Integer regionId;
    private Integer resolution;
    private Integer cloud;
    private List<String> sceneIds;
    private String dataSet;
    private List<String> bandList;

}
