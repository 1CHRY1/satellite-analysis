package nnu.mnr.satelliteresource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("nnu.mnr.satelliteresource.repository")
@EnableFeignClients(basePackages = "nnu.mnr.satelliteresource.client")
public class SatelliteResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteResourceApplication.class, args);
    }

}
