package nnu.mnr.satelliteuser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("nnu.mnr.satelliteuser.repository")
public class SatelliteUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(SatelliteUserApplication.class, args);
    }

}
