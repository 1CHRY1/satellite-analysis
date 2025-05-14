package nnu.mnr.satelliteresource.model.dto.resources;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/13 14:41
 * @Description:
 */

@Data
@Builder
public class GridBasicDTO {

    private Integer rowId;
    private Integer columnId;
    private Integer resolution;

}
