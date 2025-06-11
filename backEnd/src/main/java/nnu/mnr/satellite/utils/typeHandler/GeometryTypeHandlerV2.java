package nnu.mnr.satellite.utils.typeHandler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @name: GeometryTypeHandlerV2
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/10/2025 3:55 PM
 * @version: 1.0
 */
@Component
public class GeometryTypeHandlerV2 implements TypeHandler<Geometry> {
    private final WKTReader wktReader = new WKTReader();
    private final WKTWriter wktWriter = new WKTWriter();

    @Override
    public void setParameter(PreparedStatement ps, int i, Geometry geom, JdbcType jdbcType) throws SQLException {
        try {
            if (geom != null) {
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
            String wkt = rs.getString(columnName);
            if (wkt != null && !wkt.isEmpty()) {
                return wktReader.read(wkt);
            }
            return null;
        } catch (Exception e) {
            throw new SQLException("Error converting WKT to Geometry from column: " + columnName, e);
        }
    }

    @Override
    public Geometry getResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            String wkt = rs.getString(columnIndex);
            if (wkt != null && !wkt.isEmpty()) {
                return wktReader.read(wkt);
            }
            return null;
        } catch (Exception e) {
            throw new SQLException("Error converting WKT to Geometry at column index: " + columnIndex, e);
        }
    }

    @Override
    public Geometry getResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            String wkt = cs.getString(columnIndex);
            if (wkt != null && !wkt.isEmpty()) {
                return wktReader.read(wkt);
            }
            return null;
        } catch (Exception e) {
            throw new SQLException("Error converting WKT to Geometry at column index: " + columnIndex, e);
        }
    }
}
