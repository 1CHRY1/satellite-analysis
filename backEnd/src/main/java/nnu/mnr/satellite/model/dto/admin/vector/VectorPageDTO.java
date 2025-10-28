package nnu.mnr.satellite.model.dto.admin.vector;

import lombok.Data;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

import java.time.LocalDateTime;

@Data
public class VectorPageDTO extends PageDTO {
    private String sortField = "time"; //排序字段
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
