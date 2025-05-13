package nnu.mnr.satellitewebsocket.config.web;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/12 22:14
 * @Description:
 */
public class FeignConfig {

    @Bean
    public RequestInterceptor internalRequestInterceptor() {
        return template -> {
            template.header("X-Internal-Request", "true");
        };
    }

}
