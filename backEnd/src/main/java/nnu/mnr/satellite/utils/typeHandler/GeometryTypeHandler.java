package nnu.mnr.satellite.utils.typeHandler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 11:31
 * @Description:
 */

@Component
@MappedJdbcTypes(value = JdbcType.UNDEFINED, includeNullJdbcType = true)
@MappedTypes({Geometry.class})
public class GeometryTypeHandler implements TypeHandler<Geometry> {

    private final WKBReader wkbReader = new WKBReader();
    private final WKBWriter wkbWriter = new WKBWriter();
    private final WKTWriter wktWriter = new WKTWriter();

    public String geometryToWKT(Geometry geom) {
        if (geom != null) {
            String wkt = wktWriter.write(geom);
            return wkt;
        }
        return null;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, Geometry geom, JdbcType jdbcType) throws SQLException {
        try {
            if (geom != null) {
                // 改为使用 WKT 格式
                String wkt = wktWriter.write(geom);
                ps.setString(i, wkt);
            } else {
                ps.setString(i, null);
            }
        } catch (Exception e) {
            throw new SQLException("Error converting Geometry to WKT", e);
        }
    }

    @Override
    public Geometry getResult(ResultSet rs, String columnName) throws SQLException {
        try {
            byte[] mysqlWkb = rs.getBytes(columnName);
            if (mysqlWkb != null && mysqlWkb.length >= 4) {
                int srid = ((mysqlWkb[3] & 0xFF) << 24) | ((mysqlWkb[2] & 0xFF) << 16) |
                        ((mysqlWkb[1] & 0xFF) << 8) | (mysqlWkb[0] & 0xFF);
                byte[] wkb = Arrays.copyOfRange(mysqlWkb, 4, mysqlWkb.length);
                Geometry geom = wkbReader.read(wkb);
                geom.setSRID(srid);
                return geom;
            }
            return null;
        } catch (Exception e) {
            throw new SQLException("Error converting MySQL GEOMETRY to Geometry", e);
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
