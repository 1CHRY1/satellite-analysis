package nnu.mnr.satellite.model.vo.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/14 21:38
 * @Description:
 */

@Data
public class SceneDesVO {

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

    private String sensorName;
    private String productName;

}
