package nnu.mnr.satellite.utils.typeHandler;

import com.alibaba.fastjson2.JSONArray;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 14:11
 * @Description:
 */

@Component
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes({List.class})
public class ListTypeHandler<T> implements TypeHandler<List<T>> {

    @Override
    public void setParameter(PreparedStatement ps, int i, List<T> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, JdbcType.VARCHAR.ordinal());
            return;
        }
        JSONArray jsonArray = new JSONArray(parameter);
        ps.setString(i, jsonArray.toJSONString());
    }

    @Override
    public List<T> getResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJsonToList(json);
    }

    @Override
    public List<T> getResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJsonToList(json);
    }

    @Override
    public List<T> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJsonToList(json);
    }

    private List<T> parseJsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            JSONArray jsonArray = JSONArray.parseArray(json);
            List<T> result = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                Object value = jsonArray.get(i);
                // 关键改进：智能类型转换
                if (value != null) {
                    result.add(convertValue(value));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON to List: " + json, e);
        }
    }

    @SuppressWarnings("unchecked")
    private T convertValue(Object value) {
        // 处理数字类型的自动转换
        if (value instanceof Number) {
            Number number = (Number) value;
            // 根据实际场景调整转换逻辑
            if (number.doubleValue() % 1 == 0) {
                return (T) Integer.valueOf(number.intValue()); // 整数转Integer
            } else {
                return (T) Double.valueOf(number.doubleValue()); // 小数转Double
            }
        }
        // 其他类型直接强制转换
        return (T) value;
    }

}