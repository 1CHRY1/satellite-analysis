package nnu.mnr.satellite.model.dto.resources;

import lombok.Data;

import java.util.List;

@Data
public class GridVectorFetchDTO {
    private List<GridBasicDTO> grids;
    private List<String> TableNames;
}
