package nnu.mnr.satelliteresource.utils.typeHandler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(JSONArray.class)
public class JSONArrayTypeHandler extends BaseTypeHandler<JSONArray> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    JSONArray parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.toJSONString());
    }

    @Override
    public JSONArray getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public JSONArray getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public JSONArray getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private JSONArray parseJson(String json) {
        // 1. 空值处理
        if (json == null || json.trim().isEmpty()) {
            return new JSONArray();
        }

        // 2. 强制格式修复（处理所有已知问题）
        String fixedJson = json.trim();

        // 确保是数组格式
        if (!fixedJson.startsWith("[")) fixedJson = "[" + fixedJson;
        if (!fixedJson.endsWith("]")) fixedJson = fixedJson + "]";

        // 处理MySQL GROUP_CONCAT可能添加的转义
        fixedJson = fixedJson.replace("\\\"", "\"")
                .replace("\\n", "")
                .replace("\\r", "");

        // 3. 终极解析方案
        try {
            // 先尝试标准解析
            return JSON.parseArray(fixedJson);
        } catch (Exception e1) {
            try {
                // 应急方案：手动构造JSON
                if (fixedJson.startsWith("[{") && fixedJson.endsWith("}]")) {
                    String content = fixedJson.substring(2, fixedJson.length()-2);
                    String[] items = content.split("\\},\\s*\\{");
                    JSONArray array = new JSONArray();
                    for (String item : items) {
                        array.add(JSON.parseObject("{" + item + "}"));
                    }
                    return array;
                }
            } catch (Exception e2) {
                // 最后防线：返回空数组
                return new JSONArray();
            }
            return new JSONArray();
        }
    }
}