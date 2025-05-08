package nnu.mnr.satellitemodeling.model.dto.modeling;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/24 15:19
 * @Description:
 */

@Data
public class ProjectFileDTO {

    private String userId;
    private String projectId;
    private String path;
    private String name;
    private String content;
}
