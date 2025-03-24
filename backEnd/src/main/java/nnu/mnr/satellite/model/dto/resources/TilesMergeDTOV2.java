package nnu.mnr.satellite.model.dto.resources;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 20:43
 * @Description:
 */

@Data
public class TilesMergeDTOV2 {

    private String sceneId;
    private List<String> tiles;

}
