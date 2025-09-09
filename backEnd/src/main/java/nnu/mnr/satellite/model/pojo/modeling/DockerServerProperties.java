package nnu.mnr.satellite.model.pojo.modeling;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 23:24
 * @Description:
 */

@Component
@Data
@ConfigurationProperties(prefix = DockerServerProperties.DOCKERSERVER_PREFIX)
public class DockerServerProperties {

    public static final String DOCKERSERVER_PREFIX = "docker";

    private Map<String,String> defaultServer;

    private String localPath;

    private String serverDir;

    private String workDir;

    private ServicePortRange servicePortRange;

    private ServiceDefaults serviceDefaults;

    private Faas faas;

    @Data
    public static class ServicePortRange {
        private Integer start;
        private Integer end;
    }

    @Data
    public static class ServiceDefaults {
        private Integer internalPort;
    }

    @Data
    public static class Faas {
        private String workDirSubdir;
    }

}
