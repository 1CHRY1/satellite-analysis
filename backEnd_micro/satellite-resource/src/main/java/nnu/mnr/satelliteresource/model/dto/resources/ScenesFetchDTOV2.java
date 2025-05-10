package nnu.mnr.satelliteresource.model.dto.resources;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 20:08
 * @Description:
 */

@Data
@Builder
public class ScenesFetchDTOV2 {

    private String startTime;
    private String endTime;
    private Integer regionId;
    private Integer cloud;

}
