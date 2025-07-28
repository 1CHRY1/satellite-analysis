package nnu.mnr.satellite.model.dto.modeling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualizationTileDTO {
    private String sensorName;
    private List<Float> points;
}
