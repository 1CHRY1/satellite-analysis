package nnu.mnr.satellite.model.vo.resources;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/14 21:41
 * @Description:
 */

@Data
public class TileDesVO {

    private String tileLevel;
    private String sceneId;
    private String imageId;
    private Integer cloud;
    private Integer columnId;
    private Integer rowId;

}
