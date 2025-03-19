package nnu.mnr.satellite.model.dto.resources;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/14 21:41
 * @Description:
 */

@Data
public class TileDesDTO {

    private String tileLevel;
    private String imageId;
    private Integer columnId;
    private Integer rowId;

}
