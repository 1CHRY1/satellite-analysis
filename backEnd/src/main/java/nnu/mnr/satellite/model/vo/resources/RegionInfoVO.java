package nnu.mnr.satellite.model.vo.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/8 20:31
 * @Description:
 */

@Data
public class RegionInfoVO {

    private Integer adcode;
    private String regionName;
    private String regionLevel;
    private List<Double> center;

}
