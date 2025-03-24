package nnu.mnr.satellite.model.vo.modeling;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/24 14:38
 * @Description:
 */

@Data
@Builder
public class CodingProjectVO {

    Integer status;
    String info;
    String projectId;

}
