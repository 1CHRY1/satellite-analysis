package nnu.mnr.satelliteresource.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/12 22:14
 * @Description:
 */

@Configuration
public class FeignConfig {

    @Autowired
    private ObjectMapper objectMapper; // 会自动使用我们配置的JtsModule

    @Bean
    public MappingJackson2HttpMessageConverter feignJacksonConverter() {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    @Bean
    public RequestInterceptor internalRequestInterceptor() {
        return template -> {
            template.header("X-Internal-Request", "true");
        };
    }

}
