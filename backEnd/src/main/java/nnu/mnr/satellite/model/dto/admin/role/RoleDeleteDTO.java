package nnu.mnr.satellite.model.dto.admin.role;

import lombok.Data;

import java.util.List;

@Data
public class RoleDeleteDTO {
    private List<Integer> roleIds;
}
