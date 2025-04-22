package nnu.mnr.satellitemodeling.model.dto.resources;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/20 20:43
 * @Description:
 */

@Data
public class TilesMergeDTO {

    private List<Map<String, String>> tiles;
    private List<String> bands;

}
