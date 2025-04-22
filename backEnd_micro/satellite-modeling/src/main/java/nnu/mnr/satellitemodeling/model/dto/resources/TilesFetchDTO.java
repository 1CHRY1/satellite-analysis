package nnu.mnr.satellitemodeling.model.dto.resources;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/26 21:01
 * @Description:
 */

@Data
public class TilesFetchDTO extends TileBasicDTO{

    private String sensorId;
    private String productId;
    private String band;

}
