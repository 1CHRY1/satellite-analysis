package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.model.po.Scene;
import nnu.mnr.satellite.repository.IImageRepo;
import nnu.mnr.satellite.repository.ISceneRepo;
import nnu.mnr.satellite.utils.EPSGUtil;
import nnu.mnr.satellite.utils.GeometryUtil;
import nnu.mnr.satellite.utils.MinioUtil;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:20
 * @Description:
 */

@Service("SceneDataService")
public class SceneDataService {

    @Autowired
    MinioUtil minioUtil;

    private final ISceneRepo sceneRepo;

    public SceneDataService(ISceneRepo sceneRepo) {
        this.sceneRepo = sceneRepo;
    }

    public List<Scene> getScenesByProductId(String productId) {
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        return sceneRepo.selectList(queryWrapper);
    }

    public byte[] getPngById(String sceneId) {
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scene_id", sceneId);
        Scene scene = sceneRepo.selectOne(queryWrapper);
        return minioUtil.downloadByte(scene.getBucket(), scene.getPngPath());
    }

    public JSONObject getSceneById(String sceneId) throws IOException, FactoryException {
        Scene scene = sceneRepo.getSceneById(sceneId);
//        String geoJson = GeometryUtil.geometry2geojson(scene.getBbox(), scene.getSceneId());
        JSONObject sceneJson = new JSONObject();
//        sceneJson.put("bbox", JSONObject.parseObject(geoJson));
        sceneJson.put("sceneTime", scene.getSceneTime());
        sceneJson.put("tileLevelNum", scene.getTileLevelNum());
        sceneJson.put("tileLevels", scene.getTileLevels());
        sceneJson.put("crs", EPSGUtil.getEPSGName(scene.getCoordinateSystem()));
        sceneJson.put("bandNum", scene.getBandNum());
        sceneJson.put("bands", scene.getBands());
        sceneJson.put("description", scene.getDescription());
        return sceneJson;
    }

    public JSONArray getScenesByIdsTimeAndBBox(JSONObject params) throws IOException {
//        JSONObject params = sceneRequestResolving(paramObj);
        String sensorId = params.getString("sensorId");
        String productId = params.getString("productId");
        String start = params.getString("startTime");
        String end = params.getString("endTime");
        JSONObject geoJson = params.getJSONObject("geometry");
        String type = geoJson.getString("type");
        JSONArray coordinates = geoJson.getJSONArray("coordinates");
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        if (type.equals("Polygon")) {
            Geometry bbox = GeometryUtil.parsePolygon(coordinates, geometryFactory);
            bbox.setSRID(4326);
            QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sensor_id", sensorId);
            queryWrapper.eq("product_id", productId);
            queryWrapper.between("scene_time", start, end);

            String wkt = bbox.toText(); // 转换为 WKT 格式

            queryWrapper.apply(
                    "( ST_Intersects(ST_GeomFromText( {0}, 4326), bounding_box) OR " +
                            "ST_Contains(ST_GeomFromText( {0}, 4326), bounding_box) OR " +
                            "ST_Within(ST_GeomFromText( {0}, 4326), bounding_box) )",
                    wkt
            );
            List<Scene> sceneList = sceneRepo.selectList(queryWrapper);
            JSONArray sceneJsonList = new JSONArray();
            for (Scene scene : sceneList) {
                String sceneGeoJson = GeometryUtil.geometry2geojson(scene.getBbox(), scene.getSceneId());
                sceneJsonList.add(JSONObject.parseObject(sceneGeoJson));
            }
            return sceneJsonList;
        } else {
            // TODO: 其他的类型
            return new JSONArray();
        }
    }

//    private static JSONObject sceneRequestResolving(JSONObject paramObj) {
//        // TODO: support sensorIds and productIds
//        JSONObject params = new JSONObject();
//        params.put("sensorId", paramObj.getString("sensorId"));
//        params.put("productId", paramObj.getString("productId"));
//        params.put("startTime", paramObj.getString("startTime"));
//        params.put("endTime", paramObj.getString("endTime"));
//        params.put("bbox", paramObj.getJSONObject("geometry"));
//        return params;
//    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

}
