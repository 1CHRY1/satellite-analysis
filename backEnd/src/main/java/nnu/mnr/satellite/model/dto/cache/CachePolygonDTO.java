package nnu.mnr.satellite.model.dto.cache;

import lombok.Data;
import org.geotools.geojson.geom.GeometryJSON;

import java.util.Map;

@Data
public class CachePolygonDTO {
    private String id;
    private Map<String, Object> geoJson;
}
