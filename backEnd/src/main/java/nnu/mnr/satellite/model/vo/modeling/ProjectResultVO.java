package nnu.mnr.satellite.model.vo.modeling;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/25 10:26
 * @Description:
 */

@Data
public class ProjectResultVO {

    private String dataId;
    private String dataName;
    private String dataType;
    private LocalDateTime createTime;

}
