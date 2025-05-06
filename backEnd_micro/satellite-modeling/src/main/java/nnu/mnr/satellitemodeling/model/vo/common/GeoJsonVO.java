package nnu.mnr.satellitemodeling.model.vo.common;

import com.alibaba.fastjson2.JSONArray;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/15 15:01
 * @Description:
 */

@Data
public class GeoJsonVO {

    String type;
    JSONArray features;

}
