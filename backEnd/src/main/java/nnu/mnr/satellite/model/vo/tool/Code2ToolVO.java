package nnu.mnr.satellite.model.vo.tool;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Code2ToolVO {
    Integer status;
    String message;
    String toolId;
}
