package nnu.mnr.satellitemodeling.model.vo.resources;

import lombok.Builder;
import lombok.Data;
import nnu.mnr.satelliteresource.model.dto.modeling.ModelServerSceneDTO;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/13 14:41
 * @Description:
 */

@Data
@Builder
public class GridSceneVO {

    private Integer rowId;
    private Integer columnId;
    private Integer resolution;
    private List<ModelServerSceneDTO> scenes;

}
