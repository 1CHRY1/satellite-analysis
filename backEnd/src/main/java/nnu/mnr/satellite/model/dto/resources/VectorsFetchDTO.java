package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorsFetchDTO {
    private String startTime;
    private String endTime;
    private Integer regionId;
}
