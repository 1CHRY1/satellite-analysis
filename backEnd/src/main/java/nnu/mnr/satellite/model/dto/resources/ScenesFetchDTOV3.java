package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScenesFetchDTOV3 {

    private String startTime;
    private String endTime;
    private Integer regionId;
    private Integer resolution;

}

