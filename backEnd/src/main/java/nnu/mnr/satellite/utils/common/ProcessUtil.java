package nnu.mnr.satellite.utils.common;

import com.alibaba.fastjson2.JSONObject;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 10:32
 * @Description:
 */
public class ProcessUtil {

    public static String runModelCase(String url, JSONObject params) {
        try {
            return HttpUtil.doPost(url, params);
        } catch (Exception e) {
            // TODO: verify
            return "RUN WRONG";
        }
    }

    public static String getModelCaseStatus(String url, String caseId) {
        try {
            return HttpUtil.doGet(url, JSONObject.of("caseId", caseId));
        } catch (Exception e) {
            // TODO: verify
            return "STATUS WRONG";
        }
    }

    public static JSONObject getModelCaseResult(String url, String caseId) {
        try {
            String resultStr = HttpUtil.doGet(url, JSONObject.of("caseId", caseId));
            return JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            // TODO: verify
            return JSONObject.of("result", "RESULT WRONG");
        }
    }



}
