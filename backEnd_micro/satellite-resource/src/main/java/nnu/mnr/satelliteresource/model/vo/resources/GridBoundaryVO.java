package nnu.mnr.satelliteresource.model.vo.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/13 14:41
 * @Description:
 */

@Data
@Builder
public class GridBoundaryVO {

    private Integer rowId;
    private Integer columnId;
    private Integer resolution;
    private JSONObject boundary;

}
