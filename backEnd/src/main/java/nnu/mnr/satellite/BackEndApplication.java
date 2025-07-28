package nnu.mnr.satellite;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("nnu.mnr.satellite.mapper")
@EnableMongoRepositories(basePackages = "nnu.mnr.satellite.opengmp.repository")
@EnableElasticsearchRepositories(basePackages = "nnu.mnr.satellite.repository.resources")
@EnableScheduling
public class BackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackEndApplication.class, args);
    }

}
