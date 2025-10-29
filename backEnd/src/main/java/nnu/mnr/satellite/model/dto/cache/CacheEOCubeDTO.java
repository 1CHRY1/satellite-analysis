package nnu.mnr.satellite.model.dto.cache;

import lombok.Data;
import nnu.mnr.satellite.cache.EOCubeCache;

import java.util.List;
import java.util.Map;

@Data
public class CacheEOCubeDTO {
    private String cubeId;
    private List<String> dimensionSensors;
    private List<String> dimensionDates;
    private List<String> dimensionBands;
    private List<EOCubeCache.Scene> dimensionScenes;
}
