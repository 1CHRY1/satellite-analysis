package nnu.mnr.satellite.config.dt;

import com.alibaba.fastjson2.JSONObject;
import lombok.SneakyThrows;
import nnu.mnr.satellite.model.po.modeling.ProjectResult;
import nnu.mnr.satellite.model.vo.modeling.ProjectResultVO;
import nnu.mnr.satellite.model.po.modeling.Project;
import nnu.mnr.satellite.model.vo.modeling.ProjectVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.utils.geom.EPSGUtil;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
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

        modelMapper.typeMap(ProjectResult.class, ProjectResultVO.class)
                .addMappings(mapper -> {
                    mapper.using(geometryConverter).map(ProjectResult::getBbox, ProjectResultVO::setBbox);
                });

        // Ensure boolean isTool in Project maps to 1/0 integer in ProjectVO
        Converter<Boolean, Integer> booleanToInteger = new Converter<Boolean, Integer>() {
            @Override
            public Integer convert(MappingContext<Boolean, Integer> context) {
                Boolean src = context.getSource();
                return (src != null && src) ? 1 : 0;
            }
        };
        modelMapper.typeMap(Project.class, ProjectVO.class)
                .addMappings(mapper -> mapper.using(booleanToInteger).map(Project::isTool, ProjectVO::setIsTool));

        return modelMapper;
    }

}
