package nnu.mnr.satellite.model.vo.admin;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SceneInfoVO {
    private String sceneId;
    private String productId;
    private String sensorId;
    private String sceneName;
    private LocalDateTime sceneTime;
    private Integer tileLevelNum;

    private HashSet<String> tileLevels;

    private String coordinateSystem;

    private String description;

    private Integer bandNum;

    private HashSet<String> bands;

    private float cloud;
    private String cloudPath;
    private String bucket;

    private JSONObject tags;
    private Double noData;
}
