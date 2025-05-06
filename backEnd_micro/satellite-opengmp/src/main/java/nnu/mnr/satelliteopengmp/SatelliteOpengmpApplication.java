package nnu.mnr.satelliteopengmp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients(basePackages = "nnu.mnr.satelliteopengmp.client")
public class SatelliteOpengmpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteOpengmpApplication.class, args);
    }

}
