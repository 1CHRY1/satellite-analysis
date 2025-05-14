package nnu.mnr.satellite.model.dto.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 21:24
 * @Description:
 */

@Data
public class NdviFetchDTO {

    private Double[] point;
    private List<String> sceneIds;

}
