package nnu.mnr.satellite.model.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:30
 * @Description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(buildMethodName = "buildProduct")
public class Product {
    private String productId;
    private String sensorId;
    private String productName;
    private String description;
}
