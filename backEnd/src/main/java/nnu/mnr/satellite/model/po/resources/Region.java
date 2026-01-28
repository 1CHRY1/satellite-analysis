package nnu.mnr.satellite.model.po.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.PostgisGeometryTypeHandler;
import org.locationtech.jts.geom.Geometry;
import nnu.mnr.satellite.utils.typeHandler.ListTypeHandler;
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
    @TableField(typeHandler = ListTypeHandler.class)
    private List<Integer> acroutes;
    @TableField(typeHandler = ListTypeHandler.class)
    private List<Double> center;
    @TableField(typeHandler = ListTypeHandler.class)
    private List<Double> centroid;
    @TableField(typeHandler = PostgisGeometryTypeHandler.class)
    private Geometry boundary;
}
