package nnu.mnr.satellite.model.vo.admin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SystemInfoReportVO {
    private JSONObject cpuInfo;
    private JSONObject memoryInfo;
    private JSONArray diskInfo;
    private JSONArray networkInfo;
}
