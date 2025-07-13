package nnu.mnr.satellite.model.vo.resources;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GridVectorVO {
    private Integer rowId;
    private Integer columnId;
    private List<VectorInfoVO> vectors;
}
