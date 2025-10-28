package nnu.mnr.satellite.model.dto.admin.sensor;

import lombok.Data;

import java.util.List;

@Data
public class SensorDeleteDTO {
    private List<String> sensorIds;
}
