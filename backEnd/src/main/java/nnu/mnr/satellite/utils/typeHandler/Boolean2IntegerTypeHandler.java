package nnu.mnr.satellite.utils.typeHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Boolean2IntegerTypeHandler extends BaseTypeHandler<Boolean> {

    // 1. Java → JDBC：将 Boolean 存入数据库时，转成 1 或 0
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter ? 1 : 0); // true → 1, false → 0
    }

    // 2. JDBC → Java：从数据库读取 Integer 并转成 Boolean
    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : (value == 1); // 1 → true, 0 → false
    }

    // 3. 通过列索引读取
    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : (value == 1);
    }

    // 4. 从存储过程读取
    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : (value == 1);
    }
}
