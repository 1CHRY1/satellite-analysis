package nnu.mnr.satellite.model.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:32
 * @Description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(buildMethodName = "buildTile")
public class Tile {
    private String tileId;
    private Integer tileLevel;
    private String imageId;
    private Integer columnId;
    private Integer rowId;
    private String path;
}
