package nnu.mnr.satellite.nettywebsocket;

import nnu.mnr.satellite.nettywebsocket.support.WebSocketAnnotationPostProcessor;
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
