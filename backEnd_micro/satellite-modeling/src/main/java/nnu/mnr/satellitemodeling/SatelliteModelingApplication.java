package nnu.mnr.satellitemodeling;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@MapperScan("nnu.mnr.satellitemodeling.repository")
public class SatelliteModelingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteModelingApplication.class, args);
    }

}
