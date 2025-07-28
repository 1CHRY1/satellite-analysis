package nnu.mnr.satellite.model.vo.resources;

import lombok.Data;

@Data
public class CoverageReportWithCacheKeyVO {
    private CoverageReportVO report;
    private String encryptedRequestBody;
}
