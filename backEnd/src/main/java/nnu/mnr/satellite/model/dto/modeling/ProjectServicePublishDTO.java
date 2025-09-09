package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectServicePublishDTO extends ProjectBasicDTO {
    
    Integer servicePort;
    
}
