package nnu.mnr.satellite.model.dto.admin.role;

import lombok.Data;

@Data
public class RoleInsertDTO {
    private String name;
    private String description;
    private Integer maxCpu;
    private Integer maxStorage;
    private Integer maxJob;
    private Integer isSuperAdmin = 0;
}
