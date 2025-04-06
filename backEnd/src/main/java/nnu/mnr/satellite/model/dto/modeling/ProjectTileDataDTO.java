package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/30 22:26
 * @Description:
 */

@Data
public class ProjectTileDataDTO {

    private String userId;
    private String projectId;
    private String name;
//    private String sceneId;
//    private List<String> tileIds;
    // TODO: add another data table
    private String object;

}
