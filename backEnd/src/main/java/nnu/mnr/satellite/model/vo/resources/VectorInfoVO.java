package nnu.mnr.satellite.model.vo.resources;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VectorInfoVO {
    private String vectorName;
    private String tableName;
    private LocalDateTime time;
    private Fields fields;

    @Data
    public static class Fields {
        private List<String> continuous;
        private List<String> discrete;
    }
}
