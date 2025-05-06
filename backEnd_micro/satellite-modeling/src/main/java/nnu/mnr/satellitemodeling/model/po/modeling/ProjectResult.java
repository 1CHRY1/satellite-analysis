package nnu.mnr.satellitemodeling.model.po.modeling;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellitemodeling.utils.typeHandler.GeometryTypeHandler;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/25 10:19
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("project_data")
public class ProjectResult {

    @TableId
    private String dataId;
    private String dataName;
    private String dataType;
    private String projectId;
    private String userId;
    private String path;
    private String bucket;
    private LocalDateTime createTime;
    @TableField(value="bounding_box", typeHandler = GeometryTypeHandler.class)
    private Geometry bbox;

}
