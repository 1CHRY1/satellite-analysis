package nnu.mnr.satellite.model.vo.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.JSONArrayTypeHandler;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/14 21:38
 * @Description:
 */

@Data
public class SceneDesVO {

    private String sceneId;
    private String sceneName;
    private LocalDateTime sceneTime;
    private Integer tileLevelNum;
    private HashSet<String> tileLevels;
    private String coordinateSystem;
    private String description;
    private Integer bandNum;
    private HashSet<String> bands;
    private Integer cloud;

    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSONObject tags;
    private Double noData;

    // 外键
    private String sensorName;
    private String productName;
    private String platformName;
    private String resolution;
    private String dataType;
//    @TableField(typeHandler = JSONArrayTypeHandler.class)
//    private JSONArray images;

}
