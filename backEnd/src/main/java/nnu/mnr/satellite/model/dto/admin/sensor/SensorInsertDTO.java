package nnu.mnr.satellite.model.dto.admin.sensor;

import lombok.Data;

@Data
public class SensorInsertDTO {
    private String sensorName;
    private String platformName;
    private String description;
    private String dataType = "satellite";
}
