package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisualizationLowLevelTile {
    private String sensorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer regionId;
    private String locationId;
    private Integer resolution;
}
