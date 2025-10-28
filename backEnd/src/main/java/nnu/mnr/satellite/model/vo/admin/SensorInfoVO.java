package nnu.mnr.satellite.model.vo.admin;

import lombok.Data;

@Data
public class SensorInfoVO {
    private String sensorId;
    private String sensorName;
    private String platformName;
    private String description;
    private String dataType;
    private Integer sceneCount;
}
