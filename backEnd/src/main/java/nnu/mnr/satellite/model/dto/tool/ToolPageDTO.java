package nnu.mnr.satellite.model.dto.tool;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolPageDTO extends PageDTO {
    private String userId;
    private Integer pageSize = 12; //每页数量
}
