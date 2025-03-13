package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.model.po.Scene;
import nnu.mnr.satellite.repository.IImageRepo;
import nnu.mnr.satellite.repository.ISceneRepo;
import nnu.mnr.satellite.utils.GeometryUtil;
import nnu.mnr.satellite.utils.MinioUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKBWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Scene getSceneById(String sceneId) {
        QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scene_id", sceneId);
        return sceneRepo.selectOne(queryWrapper);
    }

    public List<Scene> getScenesByIdsTimeAndBBox(JSONObject params) {
//        JSONObject params = sceneRequestResolving(paramObj);
        String sensorId = params.getString("sensorId");
        String productId = params.getString("productId");
        String start = params.getString("startTime");
        String end = params.getString("endTime");
        JSONObject geoJson = params.getJSONObject("geometry");
        String type = geoJson.getString("type");
        JSONArray coordinates = geoJson.getJSONArray("coordinates");
        GeometryFactory geometryFactory = new GeometryFactory();
        if (type.equals("Polygon")) {
            Geometry bbox = GeometryUtil.parseLinearRing(coordinates, geometryFactory);
            QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sensor_id", sensorId);
            queryWrapper.eq("product_id", productId);
            queryWrapper.between("image_time", start, end);

            WKBWriter wkbWriter = new WKBWriter();
            byte[] wkb = wkbWriter.write(bbox);
            String wkbHex = bytesToHex(wkb);
            queryWrapper.apply("ST_Intersects(bounding_box, ST_GeomFromWKB(UNHEX({0})))", wkbHex);

            return sceneRepo.selectList(queryWrapper);
        } else {
            // TODO: 其他的类型
            return new ArrayList<>();
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
