package nnu.mnr.satelliteresource.utils.typeHandler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 14:11
 * @Description:
 */

@Component
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes({HashSet.class})
public class SetTypeHandler implements TypeHandler<HashSet<String>> {

    @Override
    public void setParameter(PreparedStatement ps, int i, HashSet<String> set, JdbcType jdbcType) throws SQLException {
        String value = String.join(",", set);
        ps.setString(i, value);
    }

    @Override
    public HashSet<String> getResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convertToSet(value);
    }

    @Override
    public HashSet<String> getResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convertToSet(value);
    }

    @Override
    public HashSet<String> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convertToSet(value);
    }

    private HashSet<String> convertToSet(String value) {
        if (value == null || value.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(value.split(",")));
    }

}
