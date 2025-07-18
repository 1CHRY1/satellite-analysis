package nnu.mnr.satellite.model.vo.modeling;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/28 15:49
 * @Description:
 */

@Data
@SuperBuilder(builderMethodName = "tilerBuilder")
public class TilerResultVO {

    private String tilerUrl;
    private String object;
    private String minioEndpoint;

}
