package nnu.mnr.satellite.model.dto.admin.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SensorPageDTO extends PageDTO {
    private Boolean asc = true; //是否顺序，从小到大
    private String sortField = "sensorName"; //排序字段
    private List<String> dataTypes;
}
