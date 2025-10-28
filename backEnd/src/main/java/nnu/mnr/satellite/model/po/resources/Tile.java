package nnu.mnr.satellite.model.po.resources;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import org.locationtech.jts.geom.Geometry;

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
//@TableName("tile_table")
public class Tile {
    @TableId
    private String tileId;
    private String tileLevel;
    private String imageId;
    private float cloud;
    private String band;
    private Integer columnId;
    private Integer rowId;
    private String path;
    private String bucket;

    @TableField(value = "bounding_box", typeHandler = GeometryTypeHandler.class)
    private Geometry bbox;
}
