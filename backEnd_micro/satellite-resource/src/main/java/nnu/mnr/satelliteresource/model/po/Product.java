package nnu.mnr.satelliteresource.model.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("product_table")
public class Product {
    @TableId
    private String productId;
    private String sensorId;
    private String productName;
    private String description;
    private String resolution;
    private String period;
}
