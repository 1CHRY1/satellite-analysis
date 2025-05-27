package nnu.mnr.satellite.utils.geom;

import nnu.mnr.satellite.model.pojo.common.TileBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Yiming
 * @Date: 2023/08/15/23:08
 * @Description:
 */
public class TileUtil {
    /**
     * @Description:瓦片转经度
     * @Author: Yiming
     * @Date: 2022/4/5
     */

    private static final Map<String, List<String>> fieldListMap = new HashMap<>(Map.of(
            "place_label_pt", List.of("width", "height", "label"),
            "river_bg_vec", List.of("height"),
            "river_land", List.of("height"),
            "river_section_label", List.of("label"),
            "river_name_label", List.of("label"),
            "mzs_place_label", List.of("label"),
            "mzs_place_line", List.of("id"),
            "mzs_bank_label", List.of("id", "label", "warn"),
            "mzs_section_line", List.of("id", "label"),
            "mzs_bank_line", List.of("id", "warn")
    ));
    static {
        fieldListMap.put("mzs_section_line_label", List.of("id", "label"));
        fieldListMap.put("mzs_bank_area_w", List.of("id", "warn"));
        fieldListMap.put("mzs_bank_area_s", List.of("id", "stability", "stability_2019"));
        fieldListMap.put("mzs_overwater_bound", List.of("id"));
        fieldListMap.put("mzs_underwater_bound", List.of("id"));
    }

    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    /**
     * @Description:瓦片转维度
     * @Author: Yiming
     * @Date: 2022/4/5
     */
    public static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /**
     * @Description:更具经纬度取得瓦片路径
     * @Author: Yiming
     * @Date: 2022/4/5
     */
    public static String getTileNumber(double lat, double lon, int zoom) {
        int x = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int y = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom));
        if(x < 0) x = 0;
        if(x >= (1 << zoom)) x = (1 << zoom) - 1;
        if(y < 0) y = 0;
        if(y >= (1 << zoom)) y = (1 << zoom) - 1;
        return zoom + "/" + x + "/" + y;
    }

    /**
     * @Description:瓦片编号获取范围(经纬度)
     * @Author: Yiming
     * @Date: 2022/4/5
     */
    public static TileBox tile2boundingBox(int x, int y, int zoom, String tableName) {
        TileBox tileBox = new TileBox();
        tileBox.setXMin(tile2lon(x, zoom));
        tileBox.setXMax(tile2lon(x + 1, zoom));
        tileBox.setYMin(tile2lat(y + 1, zoom));
        tileBox.setYMax(tile2lat(y, zoom));
        tileBox.setName(tableName);
        tileBox.setProjection(4326);
        tileBox.setFieldList(fieldListMap.get(tableName));
        return tileBox;
    }

    public static TileBox tile2boundingBox(int x, int y, int zoom, String tableName, List<String> fieldList) {
        TileBox tileBox = new TileBox();
        tileBox.setXMin(tile2lon(x, zoom));
        tileBox.setXMax(tile2lon(x + 1, zoom));
        tileBox.setYMin(tile2lat(y + 1, zoom));
        tileBox.setYMax(tile2lat(y, zoom));
        tileBox.setName(tableName);
        tileBox.setProjection(4326);
        tileBox.setFieldList(fieldList);
        return tileBox;
    }

    public static TileBox tile2Mercator(int x, int y, int zoom, String tableName) {
        TileBox tileBox = new TileBox();
        tileBox.setXMin(tile2lon(x, zoom) * 20037508.34 / 180);
        tileBox.setXMax(tile2lon(x + 1, zoom) * 20037508.34 / 180);
        tileBox.setYMin(Math.log(Math.tan((90 + tile2lat(y + 1, zoom)) * Math.PI / 360)) / (Math.PI / 180));
        tileBox.setYMax(Math.log(Math.tan((90 + tile2lat(y, zoom)) * Math.PI / 360)) / (Math.PI / 180));
        tileBox.setName(tableName);
        tileBox.setProjection(3857);
        return tileBox;
    }
}
