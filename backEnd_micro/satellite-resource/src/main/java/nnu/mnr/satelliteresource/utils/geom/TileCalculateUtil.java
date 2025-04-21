package nnu.mnr.satelliteresource.utils.geom;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

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
}
