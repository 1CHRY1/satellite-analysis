package nnu.mnr.satellitemodeling.model.vo.common;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 22:26
 * @Description:
 */

@Data
@Builder
public class CommonResultVO {

    private Integer status;
    private String message;
    private Object data;

}
