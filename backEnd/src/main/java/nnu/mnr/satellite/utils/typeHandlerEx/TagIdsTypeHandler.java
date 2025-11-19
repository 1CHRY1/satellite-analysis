package nnu.mnr.satellite.utils.typeHandlerEx;


import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class TagIdsTypeHandler extends BaseTypeHandler<List<Integer>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType) {
        // 不需要实现（仅用于读取）
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String tagIdsStr = rs.getString(columnName);
        return parseTagIds(tagIdsStr);
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String tagIdsStr = rs.getString(columnIndex);
        return parseTagIds(tagIdsStr);
    }

    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String tagIdsStr = cs.getString(columnIndex);
        return parseTagIds(tagIdsStr);
    }

    private List<Integer> parseTagIds(String tagIdsStr) {
        if (tagIdsStr == null || tagIdsStr.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagIdsStr.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }
}
