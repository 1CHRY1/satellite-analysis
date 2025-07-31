package nnu.mnr.satellite.model.dto.resources;

import lombok.Data;
import nnu.mnr.satellite.enums.common.SceneTypeByResolution;
import org.json.simple.JSONObject;

import java.util.List;

@Data
public class GridsWithFiltersDTO {
    private List<GridBasicDTO> grids;
    private SceneFilters filters;

    @Data
    public static class SceneFilters {
        private String resolutionName;
        private String source;
        private String production;
        private String category;
    }
}
