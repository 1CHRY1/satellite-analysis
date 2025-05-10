package nnu.mnr.satelliteresource.model.dto.modeling;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/9 22:46
 * @Description:
 */

@Data
@Builder
public class ModelServerImageDTO {

    private String path;
    private String band;

}
