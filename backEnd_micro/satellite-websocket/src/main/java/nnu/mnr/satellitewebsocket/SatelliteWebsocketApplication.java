package nnu.mnr.satellitewebsocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients(basePackages = "nnu.mnr.satellitewebsocket.client")
public class SatelliteWebsocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteWebsocketApplication.class, args);
    }

}
