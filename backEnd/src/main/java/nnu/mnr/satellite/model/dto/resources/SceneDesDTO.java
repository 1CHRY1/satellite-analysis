package nnu.mnr.satellite.model.dto.resources;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.SetTypeHandler;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/14 21:38
 * @Description:
 */

@Data
public class SceneDesDTO {

    private LocalDateTime sceneTime;
    private Integer tileLevelNum;
    private HashSet<String> tileLevels;
    private String crs;
    private String description;
    private Integer bandNum;
    private HashSet<String> bands;

}
