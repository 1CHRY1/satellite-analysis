package nnu.mnr.satellite.model.dto.admin.product;

import lombok.Data;

@Data
public class ProductUpdateDTO {
    private String productId;
    private String sensorId;
    private String productName;
    private String description;
    private String resolution;
    private String period;
}
