package nnu.mnr.satelliteuser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("nnu.mnr.satelliteuser.repository")
public class SatelliteUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteUserApplication.class, args);
    }

}
