package nnu.mnr.satellite.model.po.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.ListTypeHandler;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @name: Case
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/4/2025 3:18 PM
 * @version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("case_table")
public class Case {
    @TableId
    private String caseId;
    private String address;
    private String resolution;
    @TableField(typeHandler = GeometryTypeHandler.class)
    private Geometry boundary;
    @TableField(typeHandler = ListTypeHandler.class)
    private List<String> sceneList;
    private String dataSet;
    private String status;
    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSONObject result;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("region_id")
    private Integer regionId;
    @TableField(typeHandler = ListTypeHandler.class)
    private List<String> bandList;
    private String type;
    private String userId;
}
