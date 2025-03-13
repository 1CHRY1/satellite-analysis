package nnu.mnr.satellite.utils.typeHandler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 11:31
 * @Description:
 */

@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes({Geometry.class})
public class GeometryTypeHandler implements TypeHandler<Geometry> {

    private final WKBReader wkbReader = new WKBReader();
    private final WKBWriter wkbWriter = new WKBWriter();

    @Override
    public void setParameter(PreparedStatement ps, int i, Geometry geom, JdbcType jdbcType) throws SQLException {
        try {
            byte[] wkb = wkbWriter.write(geom);
            ps.setBytes(i, wkb);
        }catch (Exception e) {
            throw new SQLException("Error converting Geometry to WKB", e);
        }

    }

    @Override
    public Geometry getResult(ResultSet rs, String columnName) throws SQLException {
        try {
            byte[] wkb = rs.getBytes(columnName);
            if (wkb != null) {
                return wkbReader.read(wkb);
            }
            return null;
        } catch (Exception e) {
            throw new SQLException("Error converting WKB to Geometry", e);
        }
    }

    @Override
    public Geometry getResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            byte[] wkb = rs.getBytes(columnIndex);
            if (wkb != null) {
                return wkbReader.read(wkb);
            }
            return null;
        } catch (Exception e) {
            throw new SQLException("Error converting WKB to Geometry", e);
        }
    }

    @Override
    public Geometry getResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            byte[] wkb = cs.getBytes(columnIndex);
            if (wkb != null) {
                return wkbReader.read(wkb);
            }
            return null;
        } catch (Exception e) {
            throw new SQLException("Error converting WKB to Geometry", e);
        }
    }
}
