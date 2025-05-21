package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/17 20:05
 * @Description:
 */

@Data
public class PointRasterFetchDTO {

    private Double[] point;
    private List<String> sceneIds;

}
