package nnu.mnr.satellite.model.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("tile_table")
public class Tile {
    @TableId
    private String tileId;
    private Integer tileLevel;
    private String imageId;
    private Integer columnId;
    private Integer rowId;
    private String path;
    private String bucket;
}
