package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 14:18
 * @Description:
 */

@Service
public class ModelComputeService {

    public Double getNDVIByPoint(List<Scene> scenes) {
//        String sensorId = param.getString("sensorId");
//        String imageId = param.getString("imageId");
//        JSONObject pointJson = param.getJSONObject("geometry");
//        Point point = GeometryUtil.parsePoint(pointJson.getJSONArray("coordinates"), new GeometryFactory(new PrecisionModel(), 4326));
        return 1.1;
    }

}
