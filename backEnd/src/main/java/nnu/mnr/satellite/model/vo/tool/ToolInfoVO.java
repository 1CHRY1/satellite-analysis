package nnu.mnr.satellite.model.vo.tool;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import nnu.mnr.satellite.utils.typeHandler.ListTypeHandler;
import com.alibaba.fastjson2.JSONObject;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ToolInfoVO {
    private String toolId;
    private String projectId;
    private String userId;
    private String toolName;
    private String description;
    private List<String> tags;
    private String category;
    private List<JSONObject> parameters;
    private LocalDateTime createTime;
    private boolean isShare;
    private boolean isPublish;
}
