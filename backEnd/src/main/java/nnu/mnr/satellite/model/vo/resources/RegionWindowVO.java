package nnu.mnr.satellite.model.vo.resources;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/13 17:12
 * @Description:
 */

@Data
@Builder
public class RegionWindowVO {

    private List<Double> center;
    private List<Double> bounds;

}
