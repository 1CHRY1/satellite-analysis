package nnu.mnr.satellite.model.dto.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.SetTypeHandler;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/19 11:18
 * @Description:
 */

@Data
public class SceneImageDTO {

    @TableId
    private String sceneId;
    private String productName;
    private String sensorName;
    private String sceneName;
    private LocalDateTime sceneTime;

    private Integer bandNum;

    @TableField(value = "bands", typeHandler = SetTypeHandler.class)
    private HashSet<String> bands;

    private Integer cloud;
    private String cloudPath;
    private String bucket;
    private String resolution;

    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSONObject tags;

    private List<ModelServerImageDTO> images;

}
