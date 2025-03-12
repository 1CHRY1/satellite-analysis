package nnu.mnr.satellite.model.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:28
 * @Description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(buildMethodName = "buildSensor")
public class Sensor {
    private String sensorId;
    private String sensorName;
    private String platformName;
    private String description;
}
