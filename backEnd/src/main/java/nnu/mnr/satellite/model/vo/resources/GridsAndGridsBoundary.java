package nnu.mnr.satellite.model.vo.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GridsAndGridsBoundary {
    private List<GridBoundaryVO> grids;
    private JSONObject geoJson;
}
