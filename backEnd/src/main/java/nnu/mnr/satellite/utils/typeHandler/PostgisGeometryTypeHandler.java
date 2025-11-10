package nnu.mnr.satellite.utils.typeHandler;

import org.apache.ibatis.type.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.postgresql.util.PGobject;
import java.sql.*;

// AI写的，我一点也看不懂，但是能跑。如果哪一天这部分又出问题了，大胆地改
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(Geometry.class)
public class PostgisGeometryTypeHandler implements TypeHandler<Geometry> {

    private static final WKBReader wkbReader = new WKBReader();
    private static final WKBWriter wkbWriter = new WKBWriter(2, true); // 支持 EWKB（带 SRID）

    @Override
    public void setParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.OTHER); // PostgreSQL 的 GEOMETRY 类型对应 JDBC 的 Types.OTHER
        } else {
            PGobject pgObject = new PGobject();
            pgObject.setType("geometry");
            // 使用 EWKB 格式写入（包含 SRID 信息）
            pgObject.setValue(toPostgisGeometryText(parameter));
            ps.setObject(i, pgObject);
        }
    }

    @Override
    public Geometry getResult(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return parsePostgisGeometry(value);
    }

    @Override
    public Geometry getResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        return parsePostgisGeometry(value);
    }

    @Override
    public Geometry getResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex);
        return parsePostgisGeometry(value);
    }

    /**
     * 将数据库返回的 Geometry 对象解析为 JTS Geometry
     */
    private Geometry parsePostgisGeometry(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof PGobject) {
                PGobject pgObject = (PGobject) value;
                if ("geometry".equalsIgnoreCase(pgObject.getType())) {
                    String geomValue = pgObject.getValue();
                    // 1. 尝试解析为 HEX 格式的 EWKB（PostGIS 默认返回格式）
                    if (geomValue.matches("^[0-9A-Fa-f]+$")) {
                        byte[] wkb = hexStringToByteArray(geomValue);
                        return wkbReader.read(wkb);
                    }
                    // 2. 尝试解析为 EWKT（如 "SRID=4326;POLYGON(...)"）
                    return new WKTReader().read(geomValue);
                }
            } else if (value instanceof byte[]) {
                // 某些驱动可能直接返回二进制 EWKB
                return wkbReader.read((byte[]) value);
            }
            // 3. 尝试解析为普通 WKT
            return new WKTReader().read(value.toString());
        } catch (ParseException e) {
            throw new SQLException("Failed to parse PostGIS geometry: " + value, e);
        }
    }

    /**
     * 将 HEX 字符串转换为字节数组（用于解析 EWKB）
     */
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 将 JTS Geometry 转换为 PostGIS 可识别的文本格式（EWKT 或 WKB）
     */
    private String toPostgisGeometryText(Geometry geometry) {
        // 使用 EWKT 格式（包含 SRID），例如: "SRID=4326;POLYGON((...))"
        return "SRID=" + geometry.getSRID() + ";" + new WKTWriter().write(geometry);
    }
}