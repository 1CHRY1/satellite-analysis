package nnu.mnr.satellite.config;

import lombok.SneakyThrows;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.utils.geom.EPSGUtil;
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

        modelMapper.typeMap(Scene.class, SceneDesVO.class)
                .addMappings(mapper -> {
                    mapper.using(crsConverter).map(Scene::getCoordinateSystem, SceneDesVO::setCrs);
                });

        return modelMapper;
    }

}
