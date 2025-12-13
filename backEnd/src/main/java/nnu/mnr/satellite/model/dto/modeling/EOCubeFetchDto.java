package nnu.mnr.satellite.model.dto.modeling;

import lombok.Builder;
import lombok.Data;
import nnu.mnr.satellite.model.dto.cache.CacheEOCubeDTO;

import java.util.List;

@Data
@Builder
public class EOCubeFetchDto {
    private Integer regionId;
    private Integer resolution;
    private List<String> sceneIds;
    private EOCubeCalcDto dataSet;
    private List<String> bandList;
}