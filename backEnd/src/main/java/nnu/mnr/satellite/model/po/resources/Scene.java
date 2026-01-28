package nnu.mnr.satellite.model.po.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.*;
import nnu.mnr.satellite.utils.typeHandlerEx.PostgreJsonbTypeHandler;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 21:16
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "buildScene")
@TableName("scene_table")
public class Scene {
    @TableId
    private String sceneId;
    private String productId;
    private String sensorId;
    private String sceneName;
    private LocalDateTime sceneTime;
    private Integer tileLevelNum;

    @TableField(value = "tile_levels", typeHandler = SetTypeHandler.class)
    private HashSet<String> tileLevels;

    private String coordinateSystem;

    @TableField(value="bounding_box", typeHandler = PostgisGeometryTypeHandler.class)
    private Geometry bbox;
    private String description;

    private Integer bandNum;

    @TableField(value = "bands", typeHandler = SetTypeHandler.class)
    private HashSet<String> bands;

    private float cloud;
    private String cloudPath;
    private String bucket;

    @TableField(typeHandler = PostgreJsonbTypeHandler.class)
    private JSONObject tags;
    private Double noData;
}
