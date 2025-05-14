package nnu.mnr.satelliteresource.model.dto.resources;

import lombok.Data;

import java.util.List;

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
