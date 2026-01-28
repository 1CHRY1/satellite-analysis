package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScenesLocationFetchDTOV3 {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String locationId;
    private Integer resolution;
}
