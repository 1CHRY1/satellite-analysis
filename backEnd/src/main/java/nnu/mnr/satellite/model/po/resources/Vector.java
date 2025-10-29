package nnu.mnr.satellite.model.po.resources;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;

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
    @TableField(typeHandler = GeometryTypeHandler.class)
    private Geometry boundary;
    private LocalDateTime time;
    private Integer count;
}
