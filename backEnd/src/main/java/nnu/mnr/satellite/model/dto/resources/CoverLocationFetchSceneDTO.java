package nnu.mnr.satellite.model.dto.resources;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/21 12:56
 * @Description:
 */

@Data
public class CoverLocationFetchSceneDTO {

    private String locationId;
    private String sensorName;
    private List<String> sceneIds;
    private Integer resolution;
}
