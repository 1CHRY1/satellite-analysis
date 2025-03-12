package nnu.mnr.satellite.model.po;

import com.alibaba.fastjson2.support.geo.Geometry;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.SetTypeHandler;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 21:16
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "buildImage")
@TableName("image_table")
public class Image {
    @TableId
    private String imageId;
    private String productId;
    private String sensorId;
    private LocalDateTime datetime;
    private Integer tileLevelNum;

    @TableField(value = "tile_levels", typeHandler = SetTypeHandler.class)
    private HashSet<String> tileLevels;

    @TableField(value = "coordinate_system")
    private String crs;
    private String band;

    @TableField(value="bounding_box", typeHandler = GeometryTypeHandler.class)
    private Geometry bbox;
    private String tifPath;
    private String pngPath;
}
