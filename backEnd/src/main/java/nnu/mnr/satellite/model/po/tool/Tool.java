package nnu.mnr.satellite.model.po.tool;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.ListTypeHandler;
import org.simpleframework.xml.convert.Convert;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tool_table")
public class Tool {
    @TableId
    private String toolId;
    private String environment;
    private String userId;
    private String toolName;
    private String description;
    @TableField(typeHandler = ListTypeHandler.class)
    private List<String> tags;
    private Integer categoryId;
    @TableField(typeHandler = ListTypeHandler.class)
    private String parameters;
    private String outputType;
    @TableField("create_time")
    private LocalDateTime createTime;
}
