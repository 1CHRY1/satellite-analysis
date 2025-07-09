package nnu.mnr.satellite.model.dto.tool;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class ToolInfoDTO {
    private String toolId;
    private String environment;
    private String toolName;
    private String description;
    private List<String> tags;
    private String category;
    private List<JSONObject> parameters;
}
