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
    private String imageId;
    private String cloud;
    private String band;
    private Integer columnId;
    private Integer rowId;

}
