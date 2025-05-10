package nnu.mnr.satelliteresource.model.vo.resources;

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
