package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScenesLocationFetchDTOV3 {
    private String startTime;
    private String endTime;
    private String locationId;
    private Integer resolution;
}
