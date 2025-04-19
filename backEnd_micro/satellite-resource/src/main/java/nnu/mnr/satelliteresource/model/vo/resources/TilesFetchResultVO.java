package nnu.mnr.satelliteresource.model.vo.resources;

import lombok.experimental.SuperBuilder;
import nnu.mnr.satellite.model.vo.modeling.TilerResultVO;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/4 10:26
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "tilesFetcherBuilder")
@Data
public class TilesFetchResultVO extends TilerResultVO {

    String sceneId;
    String tileId;
    String cloud;

}
