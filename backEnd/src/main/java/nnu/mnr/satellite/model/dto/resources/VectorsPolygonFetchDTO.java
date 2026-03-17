package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VectorsPolygonFetchDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer resolution;
    private String polygonId;
}
