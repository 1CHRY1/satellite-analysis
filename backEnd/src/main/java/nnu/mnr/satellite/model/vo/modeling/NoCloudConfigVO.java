package nnu.mnr.satellite.model.vo.modeling;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import org.geotools.geojson.GeoJSON;

@Data
public class NoCloudConfigVO {
    private String sceneId;
    private String sensorName;
    private String sceneName;
    private String resolution;
    private String cloudPath;
    private String bucket;
    private String noData;
    private JSONObject bbox;
    private JSONObject path;
    private float cloud;
    private double coverage;
}
