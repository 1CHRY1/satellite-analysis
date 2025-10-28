package nnu.mnr.satellite.model.dto.admin.vector;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VectorUpdateDTO {
    private Integer id;
    private String vectorName;
    private Integer srid;
    private LocalDateTime time;
}
