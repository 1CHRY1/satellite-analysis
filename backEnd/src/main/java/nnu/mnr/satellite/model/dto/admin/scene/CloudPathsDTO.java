package nnu.mnr.satellite.model.dto.admin.scene;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CloudPathsDTO {
    private String sceneId;
    private String bucket;       // 对应scene_table中的bucket
    private String cloudPath;    // 对应scene_table中的cloud_path
}
