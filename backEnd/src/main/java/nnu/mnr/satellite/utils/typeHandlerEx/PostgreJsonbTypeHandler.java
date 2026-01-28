package nnu.mnr.satellite.utils.typeHandlerEx;

import com.alibaba.fastjson2.JSONObject;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(JSONObject.class)
@MappedJdbcTypes({JdbcType.OTHER})
public class PostgreJsonbTypeHandler extends BaseTypeHandler<JSONObject> {

    public PostgreJsonbTypeHandler() {}

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JSONObject parameter, JdbcType jdbcType) throws SQLException {
        // PostgreSQL jsonb 需要用 Types.OTHER
        ps.setObject(i, parameter.toJSONString(), Types.OTHER);
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return json == null || json.isEmpty() ? new JSONObject() : JSONObject.parseObject(json);
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return json == null || json.isEmpty() ? new JSONObject() : JSONObject.parseObject(json);
    }

    @Override
    public JSONObject getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return json == null || json.isEmpty() ? new JSONObject() : JSONObject.parseObject(json);
    }
}
