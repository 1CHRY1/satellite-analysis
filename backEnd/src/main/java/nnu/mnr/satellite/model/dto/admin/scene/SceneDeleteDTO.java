package nnu.mnr.satellite.model.dto.admin.scene;

import lombok.Data;

import java.util.List;

@Data
public class SceneDeleteDTO {
    private List<String> sceneIds;
}
