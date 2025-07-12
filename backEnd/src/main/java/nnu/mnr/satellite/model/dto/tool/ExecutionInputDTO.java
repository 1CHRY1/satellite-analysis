package nnu.mnr.satellite.model.dto.tool;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

public class ExecutionInputDTO {
    private String toolId;
    private String environment;
    private List<JSONObject> parameters;
    private String outputType;
}
