package nnu.mnr.satellite.model.dto.modeling;

import lombok.EqualsAndHashCode;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;
import lombok.Data;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MethlibPageDTO extends PageDTO {
    private List<Integer> tags;
}
