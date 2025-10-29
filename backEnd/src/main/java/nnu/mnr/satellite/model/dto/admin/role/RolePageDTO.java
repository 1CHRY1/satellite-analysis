package nnu.mnr.satellite.model.dto.admin.role;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

@EqualsAndHashCode(callSuper = true)
@Data
public class RolePageDTO extends PageDTO {
    private String sortField = "roleId";
    private Boolean asc = true;
}
