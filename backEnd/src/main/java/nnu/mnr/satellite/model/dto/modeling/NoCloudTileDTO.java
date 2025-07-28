package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 无云一版图瓦片请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoCloudTileDTO {
    
    /**
     * 传感器名称
     */
    private String sensorName;
    private String startTime;
    private String endTime;
    private List<Float> points;
}