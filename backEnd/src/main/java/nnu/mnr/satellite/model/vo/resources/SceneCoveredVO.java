package nnu.mnr.satellite.model.vo.resources;

import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Data;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;

import java.util.List;

@Data
@Builder
public class SceneCoveredVO {
    private List<ModelServerSceneDTO> sceneList;
    private JSONObject gridsBoundary;
}
