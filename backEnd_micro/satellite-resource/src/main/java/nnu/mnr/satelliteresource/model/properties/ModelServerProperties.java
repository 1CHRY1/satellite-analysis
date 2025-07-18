package nnu.mnr.satelliteresource.model.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 22:33
 * @Description:
 */
@Component
@Data
@ConfigurationProperties(prefix = ModelServerProperties.MODELSERVER_PREFIX)
public class ModelServerProperties {

    public static final String MODELSERVER_PREFIX = "modelserver";

    private String address;
    private Map<String, String> apis;
    private Map<String, Integer> interval;

}



