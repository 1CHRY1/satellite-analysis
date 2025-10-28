package nnu.mnr.satellite.model.dto.admin.sensor;

import lombok.Data;

@Data
public class SensorUpdateDTO {
    private String sensorId;
    private String sensorName;
    private String platformName;
    private String description;
    private String dataType;
}
