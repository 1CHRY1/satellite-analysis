package nnu.mnr.satelliteresource.config.dataConfig;

import com.alibaba.fastjson2.JSONObject;
import lombok.SneakyThrows;
import nnu.mnr.satelliteresource.model.po.Scene;
import nnu.mnr.satelliteresource.model.vo.resources.SceneDesVO;
import nnu.mnr.satelliteresource.utils.geom.EPSGUtil;
import nnu.mnr.satelliteresource.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/14 22:12
 * @Description:
 */

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Converter<String, String> crsConverter = new Converter<String, String>() {
            @SneakyThrows
            @Override
            public String convert(MappingContext<String, String> mappingContext) {
                return EPSGUtil.getEPSGName(mappingContext.getSource());
            }
        };

        Converter<Geometry, JSONObject> geometryConverter = new Converter<Geometry, JSONObject>() {
            @SneakyThrows
            @Override
            public JSONObject convert(MappingContext<Geometry, JSONObject> context) {
                Geometry geometry = context.getSource();
                if (geometry == null) {
                    return null;
                }

                JSONObject jsonObject = new JSONObject();

                // 根据不同的几何类型进行处理
                switch (geometry.getGeometryType()) {
                    case "Point" -> {
                        jsonObject.put("type", "Point");
                        jsonObject.put("coordinates", new double[]{
                                geometry.getCoordinate().x,
                                geometry.getCoordinate().y
                        });
                    }
                    case "LineString" -> {
                        jsonObject.put("type", "LineString");
                        jsonObject.put("coordinates", geometry.getCoordinates());
                    }
                    case "Polygon" -> {
                        jsonObject = GeometryUtil.geometry2Geojson(geometry);
                    }
                    default -> {
                        jsonObject.put("type", geometry.getGeometryType());
                        jsonObject.put("coordinates", geometry.getCoordinates());
                    }
                }

                return jsonObject;
            }
        };

        modelMapper.typeMap(Scene.class, SceneDesVO.class)
                .addMappings(mapper -> {
                    mapper.using(crsConverter).map(Scene::getCoordinateSystem, SceneDesVO::setCoordinateSystem);
                });

        return modelMapper;
    }

}
