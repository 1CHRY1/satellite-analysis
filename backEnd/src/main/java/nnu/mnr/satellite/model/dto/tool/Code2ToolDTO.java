package nnu.mnr.satellite.model.dto.tool;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import java.util.List;

@Data
public class Code2ToolDTO {

    private String projectId;
    private String userId;

    private String toolName;
    private String description;
    private List<String> tags;
    private Integer categoryId;
    private List<JSONObject> parameters;
    private Integer servicePort;
    private boolean share;
}

