package nnu.mnr.satellitemodeling.model.dto.resources;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/26 21:09
 * @Description:
 */

@Data
public class TileBasicDTO {

    private String tileLevel;
    private Integer rowId;
    private Integer columnId;

}
