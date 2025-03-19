package nnu.mnr.satellite.test;

import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import java.io.StringReader;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/13 18:37
 * @Description:
 */
public class PolygonInteractiongTest {
    public static void doPolygonsIntersect(String geoJsonStr, String wktStr) throws Exception {
        GeometryJSON geometryJson = new GeometryJSON(6);
        Geometry geoJsonGeometry = geometryJson.read(new StringReader(geoJsonStr));
        geoJsonGeometry.setSRID(4326);

        WKTReader wktReader = new WKTReader();
        Geometry wktGeometry = wktReader.read(wktStr);
        wktGeometry.setSRID(4326);

        boolean intersects = geoJsonGeometry.intersects(wktGeometry);
        System.out.println("Do the polygons intersect? " + intersects);

        if (intersects) {
            boolean geoJsonContainsWkt = geoJsonGeometry.contains(wktGeometry);
            boolean wktContainsGeoJson = wktGeometry.contains(geoJsonGeometry);

            if (geoJsonContainsWkt) {
                System.out.println("GeoJSON Polygon contains WKT Polygon");
            } else if (wktContainsGeoJson) {
                System.out.println("WKT Polygon contains GeoJSON Polygon");
            } else {
                System.out.println("Polygons intersect but neither contains the other");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String geoJsonStr = "{ \"type\": \"Polygon\", \"coordinates\": [ [ " +
                "[60.282747665106314, 57.08416417350975], " +
                "[178.25126276394468, 57.8759651848645], " +
                "[171.8239202840981, -35.208015251261465], " +
                "[61.75568031673751, -35.6444631167527], " +
                "[60.282747665106314, 57.08416417350975] ] ] }";

        String wktStr = "POLYGON((113.938600980999 31.2667309978574, 116.52510946083 31.3023454749594, " +
                "116.534361143405 29.3665961877802, 113.998134773514 29.3336247013939, " +
                "113.938600980999 31.2667309978574))";

        doPolygonsIntersect(geoJsonStr, wktStr);
    }
}
