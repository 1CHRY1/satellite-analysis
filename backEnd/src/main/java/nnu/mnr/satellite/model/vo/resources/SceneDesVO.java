package nnu.mnr.satellite.model.vo.resources;

import lombok.Data;

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
public class SceneDesVO {

    private String sceneName;
    private LocalDateTime sceneTime;
    private Integer tileLevelNum;
    private HashSet<String> tileLevels;
    private String crs;
    private String description;
    private Integer bandNum;
    private HashSet<String> bands;
    private String cloud;
    private String path;

}
