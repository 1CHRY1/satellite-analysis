package nnu.mnr.satelliteresource.model.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/28 15:05
 * @Description:
 */

@Data
@Configuration
@ConfigurationProperties(TilerProperties.DOCKERSERVER_PREFIX)
public class TilerProperties {

    public static final String DOCKERSERVER_PREFIX = "tiler";

    private String endPoint;

}
