package nnu.mnr.satellite.service.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.cache.PolygonCache;
import nnu.mnr.satellite.model.dto.cache.CachePolygonDTO;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

@Slf4j
@Transactional
@Service("PolygonDataService")
public class PolygonDataService {

    public CommonResultVO cachePolygon(String userId, CachePolygonDTO cachePolygonDTO) throws Exception {
        String polygonId = cachePolygonDTO.getId();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> geoJson = cachePolygonDTO.getGeoJson();
        Map<String, Object> geometryMap = (Map<String, Object>) geoJson.get("geometry");
        String geometryStr = mapper.writeValueAsString(geometryMap);
        Geometry geometry = GeometryUtil.geoJsonToGeometry(geometryStr);

        PolygonCache.cachePolygon(userId, polygonId, geometry);
        return CommonResultVO.builder().status(1).data(polygonId).message("缓存成功").build();
    }


}
