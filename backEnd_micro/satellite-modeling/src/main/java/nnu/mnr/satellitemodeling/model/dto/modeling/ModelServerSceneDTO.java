package nnu.mnr.satellitemodeling.model.dto.modeling;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/9 22:38
 * @Description:
 */

@Data
@Builder
public class ModelServerSceneDTO {

    private String sceneId;
    private String cloudPath;
    private List<ModelServerImageDTO> images;

}
