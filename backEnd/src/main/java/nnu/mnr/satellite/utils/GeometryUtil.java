package nnu.mnr.satellite.utils;

import com.alibaba.fastjson2.JSONArray;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 20:16
 * @Description:
 */
public class GeometryUtil {

    /**
     * 解析 LinearRing
     *
     * @param ringCoordinates  GeoJSON 中的环坐标数组
     * @param geometryFactory  JTS GeometryFactory
     * @return JTS LinearRing 对象
     */
    public static LinearRing parseLinearRing(JSONArray ringCoordinates, GeometryFactory geometryFactory) {
        List<Coordinate> coords = new ArrayList<>();
        for (int i = 0; i < ringCoordinates.size(); i++) {
            JSONArray point = ringCoordinates.getJSONArray(i);
            double x = point.getDouble(0);
            double y = point.getDouble(1);
            coords.add(new Coordinate(x, y));
        }

        // 确保环是闭合的
        if (!coords.get(0).equals(coords.get(coords.size() - 1))) {
            coords.add(coords.get(0));
        }

        return geometryFactory.createLinearRing(new CoordinateArraySequence(coords.toArray(new Coordinate[0])));
    }

}
