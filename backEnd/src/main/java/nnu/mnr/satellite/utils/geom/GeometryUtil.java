package nnu.mnr.satellite.utils.geom;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nnu.mnr.satellite.model.vo.common.GeoJsonVO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.Tile;
import nnu.mnr.satellite.utils.common.ConcurrentUtil;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static nnu.mnr.satellite.utils.geom.TileCalculateUtil.getTileGeomByIdsAndResolution;

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

        // 处理嵌套的 JSONArray，获取最内层包含点坐标的 JSONArray
        ringCoordinates = ringCoordinates.getJSONArray(0);

        List<Coordinate> coords = new ArrayList<>();
        for (int i = 0; i < ringCoordinates.size(); i++) {
            JSONArray point = ringCoordinates.getJSONArray(i);
            double longtitude = point.getDouble(0);
            double latitude = point.getDouble(1);
            coords.add(new CoordinateXY(longtitude, latitude));
        }

        // 确保环是闭合的
        if (!coords.get(0).equals(coords.get(coords.size() - 1))) {
            coords.add(coords.get(0));
        }

        Coordinate[] coordArray = coords.toArray(new Coordinate[0]);
        CoordinateArraySequence coordSeq = new CoordinateArraySequence(coordArray);
        return geometryFactory.createLinearRing(coordSeq);
    }

    private static LinearRing createLinearRing(JSONArray ringCoordinates, GeometryFactory geometryFactory) {
        List<Coordinate> coords = new ArrayList<>();
        for (int i = 0; i < ringCoordinates.size(); i++) {
            JSONArray point = ringCoordinates.getJSONArray(i);
            double longitude = point.getDouble(0); // longitude 在前
            double latitude = point.getDouble(1);  // latitude 在后
            coords.add(new CoordinateXY(longitude, latitude));
        }

        // 确保环是闭合的
        if (!coords.get(0).equals(coords.get(coords.size() - 1))) {
            coords.add(coords.get(0));
        }

        Coordinate[] coordArray = coords.toArray(new Coordinate[0]);
        return geometryFactory.createLinearRing(coordArray);
    }

    public static Polygon parse4326Polygon(JSONArray coordinates, GeometryFactory geometryFactory) {
        // 外部环
        JSONArray outerRingCoords = coordinates.getJSONArray(0);
        LinearRing outerRing = createLinearRing(outerRingCoords, geometryFactory);

        // 内部环（洞）
        LinearRing[] innerRings = new LinearRing[coordinates.size() - 1];
        for (int i = 1; i < coordinates.size(); i++) {
            JSONArray innerRingCoords = coordinates.getJSONArray(i);
            innerRings[i - 1] = createLinearRing(innerRingCoords, geometryFactory);
        }

        return geometryFactory.createPolygon(outerRing, innerRings);
    }

    public static Geometry parse4326Point(Double[] coordinates) {

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        // 点的坐标通常是 [x, y] 形式，这里直接获取 x 和 y 坐标
        double x = coordinates[0];
        double y = coordinates[1];

        // 使用 GeometryFactory 创建 Point 对象
        return geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(x, y));
    }

    public static JSONObject geometry2Geojson(Geometry jtsGeometry) throws IOException {
        if (jtsGeometry == null) {
            return null;
        }

        // 创建 GeometryJSON 对象，指定精度（例如 6 位小数）
        GeometryJSON geometryJson = new GeometryJSON(6);

        // 将 JTS Geometry 转换为 GeoJSON 几何部分的字符串
        StringWriter writer = new StringWriter();
        geometryJson.write(jtsGeometry, writer);
        String geometryJsonStr = writer.toString();

        // 使用 Jackson 创建完整的 GeoJSON Feature 对象
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode featureNode = mapper.createObjectNode();

        featureNode.put("type", "Feature");
        featureNode.set("geometry", mapper.readTree(geometryJsonStr)); // 解析 geometry JSON

        // 转换为字符串
        return JSONObject.parseObject(mapper.writeValueAsString(featureNode));
    }

    public static JSONObject geometry2ResourceGeojson(Geometry jtsGeometry, String id) throws IOException {
        return geometry2ResourceGeojson(jtsGeometry, id, -1, -1);
    }
    public static JSONObject geometry2ResourceGeojson(Geometry jtsGeometry, String id, int columnId, int rowId) throws IOException {
        if (jtsGeometry == null || id == null) {
            return null;
        }

        // 创建 GeometryJSON 对象，指定精度（例如 6 位小数）
        GeometryJSON geometryJson = new GeometryJSON(6);

        // 将 JTS Geometry 转换为 GeoJSON 几何部分的字符串
        StringWriter writer = new StringWriter();
        geometryJson.write(jtsGeometry, writer);
        String geometryJsonStr = writer.toString();

        // 使用 Jackson 创建完整的 GeoJSON Feature 对象
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode featureNode = mapper.createObjectNode();

        // 设置 Feature 的字段
        featureNode.put("id", id);
        featureNode.put("type", "Feature");
        if (columnId != -1 && rowId != -1) {
            featureNode.set("properties", mapper.createObjectNode().put("id", id));
        } else {
            featureNode.set("properties", mapper.createObjectNode().put("id", id)
                    .put("columnId", columnId).put("rowId", rowId));
        }
        featureNode.set("geometry", mapper.readTree(geometryJsonStr)); // 解析 geometry JSON

        // 转换为字符串
        return JSONObject.parseObject(mapper.writeValueAsString(featureNode));
    }

    public static GeoJsonVO sceneList2GeojsonVO(List<Scene> items) throws IOException {
        GeoJsonVO geoJsonVO = new GeoJsonVO();
        geoJsonVO.setType("FeatureCollection");

        JSONArray features = new JSONArray();
        if (items != null) {
            List<JSONObject> result = ConcurrentUtil.processConcurrently(
                    items, item -> {
                        try {
                            return geometry2ResourceGeojson(item.getBbox(), item.getSceneId());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            features.addAll(result);
        }
        geoJsonVO.setFeatures(features);
        return geoJsonVO;
    }

    public static GeoJsonVO tileList2GeojsonVO(List<Tile> items) throws IOException {
        GeoJsonVO geoJsonVO = new GeoJsonVO();
        geoJsonVO.setType("FeatureCollection");

        JSONArray features = new JSONArray();
        if (items != null && !items.isEmpty()) {
            List<JSONObject> result = ConcurrentUtil.processConcurrently(
                    items, item -> {
                        try {
                            return geometry2ResourceGeojson(item.getBbox(), item.getTileId(), item.getColumnId(), item.getRowId());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            features.addAll(result);
        }
        geoJsonVO.setFeatures(features);
        return geoJsonVO;
    }

    public static List<Double> getGeometryBounds(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            throw new IllegalArgumentException("Geometry cannot be null or empty");
        }

        // 获取几何的包络线（Envelope）
        Envelope envelope = geometry.getEnvelopeInternal();

        // 返回左下角和右上角坐标
        return Arrays.asList(
                envelope.getMinX(), // 左下角X
                envelope.getMinY(), // 左下角Y
                envelope.getMaxX(), // 右上角X
                envelope.getMaxY()  // 右上角Y
        );
    }

    // 获取格网边界，采用级联并简化格网
    public static Geometry getGridsBoundaryByTilesAndResolution(List<Integer[]> tileIds, Integer resolution) {
        if (tileIds == null || tileIds.isEmpty()) {
            throw new IllegalArgumentException("tileIds cannot be null or empty");
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Polygon> polygons = new ArrayList<>();

        // 1. 收集所有 Polygon（包括从 MultiPolygon 拆解的）
        for (Integer[] tileId : tileIds) {
            Geometry gridGeom = getTileGeomByIdsAndResolution(tileId[1], tileId[0], resolution);
            if (gridGeom == null || gridGeom.isEmpty()) {
                throw new IllegalArgumentException("Invalid tile bounding box for tileId: [" + tileId[0] + ", " + tileId[1] + "]");
            }

            if (gridGeom instanceof Polygon) {
                polygons.add((Polygon) gridGeom);
            } else if (gridGeom instanceof MultiPolygon) {
                for (int i = 0; i < gridGeom.getNumGeometries(); i++) {
                    polygons.add((Polygon) gridGeom.getGeometryN(i));
                }
            } else {
                throw new IllegalArgumentException("Unsupported geometry type: " + gridGeom.getClass().getName());
            }
        }

        // 2. 处理单元素情况
        Geometry unionResult;
        if (polygons.size() == 1) {
            unionResult = polygons.get(0);
        } else {
            unionResult = CascadedPolygonUnion.union(polygons);
        }

        // 3. 确保返回 MultiPolygon（可选）
        Geometry result;
        if (unionResult instanceof Polygon) {
            result = geometryFactory.createMultiPolygon(new Polygon[]{(Polygon) unionResult});
        } else if (unionResult instanceof MultiPolygon) {
            result = unionResult;
        } else {
            throw new IllegalStateException("Unexpected union result type: " + unionResult.getClass().getName());
        }

        // 4. 对合并后的几何图形进行 Douglas-Peucker 简化
        double tolerance = 0.001; // 可调整的简化容差
        Geometry simplifiedResult = DouglasPeuckerSimplifier.simplify(result, tolerance);

        // 5. 确保简化后仍然是 MultiPolygon（可选）
        if (simplifiedResult instanceof Polygon) {
            return geometryFactory.createMultiPolygon(new Polygon[]{(Polygon) simplifiedResult});
        } else {
            return simplifiedResult;
        }
    }

    public static Polygon pointsConvertToPolygon(List<Float> points) {
        GeometryFactory geometryFactory = new GeometryFactory();
        // 左下
        Float minLon = points.get(0);
        Float minLat = points.get(1);
        // 右上
        Float maxLon = points.get(2);
        Float maxLat = points.get(3);
        // 按顺时针或逆时针顺序定义多边形顶点（闭合环）
        Coordinate[] coordinates = new Coordinate[] {
                new Coordinate(minLon, minLat), // 左下角
                new Coordinate(minLon, maxLat), // 左上角
                new Coordinate(maxLon, maxLat), // 右上角
                new Coordinate(maxLon, minLat), // 右下角
                new Coordinate(minLon, minLat)  // 闭合回起点
        };

        return geometryFactory.createPolygon(coordinates);
    }


}
