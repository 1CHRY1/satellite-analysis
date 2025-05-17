package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/17 20:05
 * @Description:
 */

@Data
public class SpectrumFetchDTO {

    private Double[] point;
    private String sceneId;

}
