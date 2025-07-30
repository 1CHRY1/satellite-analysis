package nnu.mnr.satellite.model.dto.cache;

import lombok.Builder;
import lombok.Data;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;

import java.util.List;

@Data
@Builder
public class CacheDataDTO<T> {
    CoverageReportVO<T> report;
    List<SceneDesVO> scenesInfo;
}
