package nnu.mnr.satellite.model.vo.resources;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GridsScenesOverlapVO {

    private List<GridsInfo> grids;
    private List<SceneInfo> scenes;

    @Data
    @Builder
    public static class GridsInfo{
        private Integer columnId;  // 网格列ID（或 tileX）
        private Integer rowId;     // 网格行ID（或 tileY）
        private Integer resolution;
        private Boolean isOverlapped; // 是否重叠
    }

    @Data
    @Builder
    public static class SceneInfo {
        private String sceneId;
        private String platformName;
    }
}
