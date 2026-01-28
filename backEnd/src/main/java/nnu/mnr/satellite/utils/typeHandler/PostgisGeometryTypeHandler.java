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

    @Override
    public void setParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.OTHER);
        } else {
            PGobject pgObject = new PGobject();
            pgObject.setType("geometry");
            pgObject.setValue("SRID=" + parameter.getSRID() + ";" + parameter.toText());
            ps.setObject(i, pgObject);
        }
    }

    @Override
    public Geometry getResult(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        return parse(obj);
    }

    @Override
    public Geometry getResult(ResultSet rs, int columnIndex) throws SQLException {
        Object obj = rs.getObject(columnIndex);
        return parse(obj);
    }

    @Override
    public Geometry getResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object obj = cs.getObject(columnIndex);
        return parse(obj);
    }

    private Geometry parse(Object obj) throws SQLException {
        if (obj == null) return null;

        try {
            if (obj instanceof byte[]) {
                return wkbReader.read((byte[]) obj);
            } else if (obj instanceof PGobject) {
                String val = ((PGobject) obj).getValue();
                if (val != null) {
                    // 如果是 EWKB hex string
                    if (val.matches("^[0-9A-Fa-f]+$")) {
                        return wkbReader.read(hexStringToByteArray(val));
                    }
                    // 如果是 EWKT 文本
                    if (val.startsWith("SRID=")) {
                        String wkt = val.substring(val.indexOf(";") + 1);
                        Geometry g = new WKTReader().read(wkt);
                        g.setSRID(Integer.parseInt(val.substring(5, val.indexOf(";"))));
                        return g;
                    }
                    // fallback
                    return new WKTReader().read(val);
                }
            }
            return new WKTReader().read(obj.toString());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to parse PostGIS geometry, ignored. value=" + obj);
            return null;
        }
    }

    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}