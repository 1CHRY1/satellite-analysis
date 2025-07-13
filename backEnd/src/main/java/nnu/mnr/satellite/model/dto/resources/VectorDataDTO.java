package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorDataDTO {
    private String tableName;
    private Integer regionId;
}
