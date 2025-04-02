package nnu.mnr.satellite.model.vo.modeling;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/28 15:49
 * @Description:
 */

@Data
@Builder
public class TilerVO {

    private String tilerUrl;
    private String object;

}
