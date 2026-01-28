package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VectorsFetchDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer regionId;
}
