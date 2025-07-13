package nnu.mnr.satellite.model.pojo.modeling;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Data
@ConfigurationProperties(prefix = SRModelServerProperties.SRMODELSERVER_PREFIX)
public class SRModelServerProperties {
    public static final String SRMODELSERVER_PREFIX = "srmodelserver";

    private String address;
    private Map<String, String> apis;
    private Map<String, Integer> interval;
}
