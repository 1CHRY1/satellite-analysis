package nnu.mnr.satellitewebsocket.nettywebsocket;

import nnu.mnr.satellitewebsocket.nettywebsocket.support.WebSocketAnnotationPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyWebsocketAutoConfiguration {

    @Bean
    public WebSocketAnnotationPostProcessor webSocketAnnotationPostProcessor() {
        return new WebSocketAnnotationPostProcessor();
    }

    @Bean
    public WebsocketProperties websocketProperties() {
        return new WebsocketProperties();
    }
}
