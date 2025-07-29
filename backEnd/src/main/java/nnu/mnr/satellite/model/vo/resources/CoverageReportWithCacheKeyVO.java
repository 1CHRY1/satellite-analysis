package nnu.mnr.satellite.model.vo.resources;

import lombok.Data;

@Data
public class CoverageReportWithCacheKeyVO<T> {
    private CoverageReportVO<T> report;
    private String encryptedRequestBody;
}
