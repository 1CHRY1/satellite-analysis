package nnu.mnr.satellitegateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SatelliteGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteGatewayApplication.class, args);
    }

}
