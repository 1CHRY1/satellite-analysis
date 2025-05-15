package nnu.mnr.satellite.model.po.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.SetTypeHandler;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/15 17:52
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "buildSceneV2")
@TableName("scene_table")
public class SceneSP {
    @TableId
    private String sceneId;
    private String productName;
    private String sensorName;
    private String sceneName;
    private LocalDateTime sceneTime;
    private String coordinateSystem;

    @TableField(value="bounding_box", typeHandler = GeometryTypeHandler.class)
    private Geometry bbox;

    private Integer bandNum;

    @TableField(value = "bands", typeHandler = SetTypeHandler.class)
    private HashSet<String> bands;

    private Integer cloud;
    private String cloudPath;
    private String bucket;

    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSONObject tags;
}
