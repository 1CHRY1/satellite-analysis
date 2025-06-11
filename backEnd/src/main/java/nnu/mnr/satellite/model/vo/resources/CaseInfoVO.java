package nnu.mnr.satellite.model.vo.resources;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.ListTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CaseInfoVO {
    private String caseId;
    private String caseName;
    private String resolution;
    private List<String> sceneList;
    private String dataSet;
    private String status;
    private JSONObject result;
    private LocalDateTime createTime;
}
