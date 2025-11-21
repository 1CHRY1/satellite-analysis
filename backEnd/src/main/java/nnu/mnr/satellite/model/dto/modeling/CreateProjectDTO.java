package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 22:25
 * @Description:
 */

@Data
public class CreateProjectDTO {

    private String projectName;
    private String environment;
    private String userId;
    private String description;
    private boolean tool;
    private String level;

}
