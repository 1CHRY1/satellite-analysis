package nnu.mnr.satelliteresource.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satelliteresource.model.dto.resources.ScenesFetchDTO;
import nnu.mnr.satelliteresource.model.po.Scene;
import nnu.mnr.satelliteresource.model.vo.common.GeoJsonVO;
import nnu.mnr.satelliteresource.model.vo.resources.SceneDesVO;
import nnu.mnr.satelliteresource.repository.ISceneRepo;
import nnu.mnr.satelliteresource.utils.data.MinioUtil;
import nnu.mnr.satelliteresource.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.modelmapper.ModelMapper;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private ModelMapper sceneModelMapper;

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

    public SceneDesVO getSceneById(String sceneId) {
        return sceneRepo.getScenesDesById(sceneId);
    }

    public GeoJsonVO getScenesByIdsTimeAndBBox(ScenesFetchDTO scenesFetchDTO) throws IOException {
        String sensorId = scenesFetchDTO.getSensorId();
        String productId = scenesFetchDTO.getProductId();
        String start = scenesFetchDTO.getStartTime();
        String end = scenesFetchDTO.getEndTime();
        JSONObject geometry = scenesFetchDTO.getGeometry();
        String type = geometry.getString("type");
        JSONArray coordinates = geometry.getJSONArray("coordinates");
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        if (type.equals("Polygon")) {
            Geometry bbox = GeometryUtil.parse4326Polygon(coordinates, geometryFactory);
            QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sensor_id", sensorId);
            queryWrapper.eq("product_id", productId);
            queryWrapper.between("scene_time", start, end);

            String wkt = bbox.toText(); // 转换为 WKT 格式

            queryWrapper.apply(
                    "( ST_Intersects(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                            "ST_Contains(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                            "ST_Within(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) )",
                    wkt
            );
            List<Scene> sceneList = sceneRepo.selectList(queryWrapper);
            return GeometryUtil.sceneList2GeojsonVO(sceneList);
        } else {
            // TODO: 其他的类型
            return new GeoJsonVO();
        }
    }

    public List<Scene> getScenesByBBox(String sensorId, String productId, JSONObject geometry) throws IOException {
        String type = geometry.getString("type");
        JSONArray coordinates = geometry.getJSONArray("coordinates");
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        if (type.equals("Polygon")) {
            Geometry bbox = GeometryUtil.parse4326Polygon(coordinates, geometryFactory);
            QueryWrapper<Scene> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sensor_id", sensorId);
            queryWrapper.eq("product_id", productId);

            String wkt = bbox.toText(); // 转换为 WKT 格式

            queryWrapper.apply(
                    "( ST_Intersects(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                            "ST_Contains(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
                            "ST_Within(ST_GeomFromText( {0}, 4326, 'axis-order=long-lat'), bounding_box) )",
                    wkt
            );
            return sceneRepo.selectList(queryWrapper);
        } else {
            // TODO: 其他的类型
            return null;
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
