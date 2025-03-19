package nnu.mnr.satellite.config;

import nnu.mnr.satellite.model.pojo.websocket.WsProperties;
import nnu.mnr.satellite.websocket.support.WsAnnotationPostProcesser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/18 22:42
 * @Description:
 */

@Configuration
public class WebSocketConfig {

    @Bean
    public WsAnnotationPostProcesser webSocketAnnotationPostProcessor() {
        return new WsAnnotationPostProcesser();
    }

    @Bean
    public WsProperties WsProperties() {
        return new WsProperties();
    }

}
