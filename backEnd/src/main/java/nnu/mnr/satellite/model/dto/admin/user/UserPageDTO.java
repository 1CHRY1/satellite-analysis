package nnu.mnr.satellite.model.dto.admin.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserPageDTO extends PageDTO {
    private List<Integer> roleIds;
}
