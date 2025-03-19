package nnu.mnr.satellite.test;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/13 14:56
 * @Description:
 */
public class GeometryTest {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://223.2.34.7:3306/satellite";
        String user = "root";
        String password = "root";

        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT bounding_box FROM scene_table WHERE scene_id = '5c52e91f-6875-4566-982f-a4612f973eb6'");

        if (rs.next()) {
            byte[] geomBytes = rs.getBytes("bounding_box");
            if (geomBytes != null && geomBytes.length >= 4) {
                int srid = ((geomBytes[3] & 0xFF) << 24) |
                        ((geomBytes[2] & 0xFF) << 16) |
                        ((geomBytes[1] & 0xFF) << 8) |
                        (geomBytes[0] & 0xFF);
                byte[] wkb = Arrays.copyOfRange(geomBytes, 4, geomBytes.length);
                WKBReader wkbReader = new WKBReader();
                Geometry geometry = wkbReader.read(wkb);
                geometry.setSRID(srid);
                System.out.println("Geometry: " + geometry);
                System.out.println("SRID: " + geometry.getSRID());
            } else {
                System.out.println("No geometry data or invalid format");
            }
        } else {
            System.out.println("No record found");
        }

        rs.close();
        stmt.close();
        conn.close();
    }

}
