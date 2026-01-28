package nnu.mnr.satellite.model.dto.resources;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 20:08
 * @Description:
 */

@Data
@Builder
public class ScenesLocationFetchDTO {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String locationId;
    private Integer resolution;
    private float cloud;

}
