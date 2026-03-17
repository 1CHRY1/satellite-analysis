package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScenesPolygonFetchDTOV3 {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String polygonId;
    private Integer resolution;
}
