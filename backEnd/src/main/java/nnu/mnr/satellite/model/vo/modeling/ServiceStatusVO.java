package nnu.mnr.satellite.model.vo.modeling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: ZZW
 * @Date: 2025/1/16
 * @Description: Response VO for service status
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceStatusVO {
    
    Boolean isPublished;
    String status;
    String url;
    
}
