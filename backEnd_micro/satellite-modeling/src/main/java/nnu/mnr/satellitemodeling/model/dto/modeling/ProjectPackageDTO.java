package nnu.mnr.satellitemodeling.model.dto.modeling;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/25 9:41
 * @Description:
 */

@Data
public class ProjectPackageDTO {

    private String userId;
    private String projectId;
    private String action;
    private String name;
    private String version;

}
