package nnu.mnr.satellitemodeling.utils.geom;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/5 18:04
 * @Description:
 */
public class TileCalculateUtil {

    public static List<Integer[]> getRowColByRegionAndResolution(Geometry region, Integer resolution) {
//        List<Geometry> tileGeoms = new ArrayList<>();
        List<Integer[]> tileIds = new ArrayList<>();
        int[] gridNum = getGridNumFromTileResolution(resolution);
        int gridNumX = gridNum[0];
        int gridNumY = gridNum[1];

        Envelope env = region.getEnvelopeInternal();
        double minLng = env.getMinX();
        double minLat = env.getMinY();
        double maxLng = env.getMaxX();
        double maxLat = env.getMaxY();

        // 将边界框的经纬度转换为网格索引
        int startRow = (int) Math.floor((90.0 - maxLat) * gridNumY / 180.0); // 顶部纬度对应的行
        int endRow = (int) Math.ceil((90.0 - minLat) * gridNumY / 180.0);   // 底部纬度对应的行
        int startCol = (int) Math.floor((minLng + 180.0) * gridNumX / 360.0); // 左侧经度对应的列
        int endCol = (int) Math.ceil((maxLng + 180.0) * gridNumX / 360.0);   // 右侧经度对应的列

        // 限制索引范围，避免越界
        startRow = Math.max(0, startRow);
        endRow = Math.min(gridNumY, endRow);
        startCol = Math.max(0, startCol);
        endCol = Math.min(gridNumX, endCol);

        GeometryFactory geometryFactory = region.getFactory();

        // 遍历网格，生成瓦片几何
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
//                Geometry tileGeom = getTileGeomByIds(row, col, gridNumX, gridNumY);
//                if (region.intersects(tileGeom)) {
//                    tileGeoms.add(tileGeom);
//                }
                // 计算当前网格的地理边界
                double tileMinLat = 90.0 - (row + 1) * 180.0 / gridNumY;
                double tileMaxLat = 90.0 - row * 180.0 / gridNumY;
                double tileMinLng = col * 360.0 / gridNumX - 180.0;
                double tileMaxLng = (col + 1) * 360.0 / gridNumX - 180.0;

                // 创建当前网格的多边形表示
                Coordinate[] coords = new Coordinate[5];
                coords[0] = new Coordinate(tileMinLng, tileMinLat);
                coords[1] = new Coordinate(tileMinLng, tileMaxLat);
                coords[2] = new Coordinate(tileMaxLng, tileMaxLat);
                coords[3] = new Coordinate(tileMaxLng, tileMinLat);
                coords[4] = new Coordinate(tileMinLng, tileMinLat); // 闭合环
                Polygon tilePolygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(coords), null);

                // 精确判断覆盖关系
                if (region.covers(tilePolygon) || region.intersects(tilePolygon)) {
                    tileIds.add(new Integer[]{col, row});
                }
            }
        }

//        return tileGeoms;
        return tileIds;
    }

    public static JSONObject getTileGeomByIds(Integer rowId, Integer columnId, Integer gridNumX, Integer gridNumY) {
        List<Double> rightLngBottomLat = grid2lnglat(rowId + 1, columnId + 1, gridNumX, gridNumY);
        List<Double> leftLngTopLat = grid2lnglat(rowId, columnId, gridNumX, gridNumY);

        Double rightLng = rightLngBottomLat.get(0);
        Double bottomLat = rightLngBottomLat.get(1);
        Double leftLng = leftLngTopLat.get(0);
        Double topLat = leftLngTopLat.get(1);

        JSONObject polygon = new JSONObject();
        polygon.put("type", "Polygon");

        JSONArray coordinates = new JSONArray();
        JSONArray ring = new JSONArray();

        ring.add(new Double[]{leftLng, topLat});
        ring.add(new Double[]{rightLng, topLat});
        ring.add(new Double[]{rightLng, bottomLat});
        ring.add(new Double[]{leftLng, bottomLat});
        ring.add(new Double[]{leftLng, topLat}); // 闭合多边形

        coordinates.add(ring);
        polygon.put("coordinates", coordinates);

        return polygon;

    }

    private static List<Double> grid2lnglat(Integer gridY, Integer gridX, Integer gridNumX, Integer gridNumY) {
        List<Double> lngLat = new ArrayList<>();
        Double lng = ((double)gridX / gridNumX) * 360.0 - 180.0;
        Double lat = 90.0 - ((double)gridY / gridNumY) * 180.0;
        lngLat.add(lng); lngLat.add(lat);
        return lngLat;
    }

    public static int[] getGridNumFromTileLevel(String tileLevel) {
        String[] parts = tileLevel.split("\\*");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid grid string format. Expected 'X*Y'");
        }
        // 转换为整数
        int gridNumX = Integer.parseInt(parts[0].trim());
        int gridNumY = Integer.parseInt(parts[1].trim());

        return new int[]{gridNumX, gridNumY};
    }

    public static int[] getGridNumFromTileResolution(Integer resolution) {
        final double EARTH_CIRCUMFERENCE_EQUATOR = 40075.0;
        final double EARTH_CIRCUMFERENCE_MERIDIAN = 40008.0;

        double degreePerGridX = (360.0 * resolution) / EARTH_CIRCUMFERENCE_EQUATOR;
        double degreePerGridY = (180.0 * resolution) / EARTH_CIRCUMFERENCE_MERIDIAN;

        int gridNumX = (int) Math.ceil(360.0 / degreePerGridX);
        int gridNumY = (int) Math.ceil(180.0 / degreePerGridY);

        return new int[]{gridNumX, gridNumY};
    }
}
