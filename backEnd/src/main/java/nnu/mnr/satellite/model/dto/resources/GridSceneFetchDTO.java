package nnu.mnr.satellite.model.dto.resources;

import lombok.Data;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/13 15:17
 * @Description:
 */

@Data
public class GridSceneFetchDTO {

    private List<GridBasicDTO> grids;
    private List<String> sceneIds;

}
