package nnu.mnr.satellite.model.vo.modeling;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: ZZW
 * @Date: 2025/1/16
 * @Description: Response VO for service status
 */

@Data
@Builder
public class ServiceStatusVO {
    
    Boolean isPublished;
    Boolean running;
    String url;
    String host;
    Integer port;
    
}
