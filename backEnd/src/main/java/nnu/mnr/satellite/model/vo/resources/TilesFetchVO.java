package nnu.mnr.satellite.model.vo.resources;

import lombok.*;
import lombok.experimental.SuperBuilder;
import nnu.mnr.satellite.model.dto.resources.TilesFetchDTO;
import nnu.mnr.satellite.model.vo.modeling.TilerVO;

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
public class TilesFetchVO extends TilerVO {

    String sceneId;
    String tileId;
    String cloud;

}
