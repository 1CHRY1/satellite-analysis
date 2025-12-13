package nnu.mnr.satellite.model.dto.modeling;

import lombok.Builder;
import lombok.Data;
import nnu.mnr.satellite.cache.EOCubeCache;
import nnu.mnr.satellite.model.dto.cache.CacheEOCubeDTO;

@Data
@Builder
public class EOCubeCalcDto extends CacheEOCubeDTO {
    private String resample;
    private String period;
}
