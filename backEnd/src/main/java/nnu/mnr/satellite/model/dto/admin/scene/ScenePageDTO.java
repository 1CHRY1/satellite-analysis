package nnu.mnr.satellite.model.dto.admin.scene;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class ScenePageDTO extends PageDTO {

    private Integer pageSize = 20; //每页数量
    private String sortField = "sceneTime"; //排序字段
    private List<String> sensorIds;
    private String productId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
