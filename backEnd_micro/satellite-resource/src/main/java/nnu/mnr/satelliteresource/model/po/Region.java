package nnu.mnr.satelliteresource.model.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/8 17:26
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "buildRegion")
@TableName("region_table")
public class Region {

    @TableId
    private Integer adcode;
    private String regionName;
    private String regionLevel;
    private String childrenNum;
    private Integer parent;
    private String subFeatureIndex;
    private List<Integer> acroutes;
    private List<Double> center;
    private List<Double> centroid;
    private Geometry boundary;

}
