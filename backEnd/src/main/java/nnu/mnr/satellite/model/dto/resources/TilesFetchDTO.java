package nnu.mnr.satellite.model.dto.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/26 21:01
 * @Description:
 */

@Data
public class TilesFetchDTO extends TileBasicDTO{

    private String sensorId;
    private String productId;
    private String band;

}
