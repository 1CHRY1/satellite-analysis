package nnu.mnr.satellite.model.po.resources;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.PostgisGeometryTypeHandler;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "vector_datasets", schema = "gis_db")
public class Vector {
    @TableId
    private Integer id;
    private String vectorName;
    private String tableName;
    private Integer srid;
    @TableField(typeHandler = PostgisGeometryTypeHandler.class)
    private Geometry boundary;
    private LocalDateTime time;
    private Integer count;
}
