package nnu.mnr.satellite.model.vo.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductInfoWithSensorVO {
    private String productId;
    private String sensorId;
    private String productName;
    private String description;
    private String resolution;
    private String period;
    private String sensorName;
}
