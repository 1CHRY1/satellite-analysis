package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;

@Data
public class VisualizationLowLevelTile {
    private String sensorName;
    private String startTime;
    private String endTime;
    private Integer regionId;
}
