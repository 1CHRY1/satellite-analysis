package nnu.mnr.satellitemodeling.client;

import feign.Headers;
import nnu.mnr.satellitemodeling.config.web.FeignConfig;
import nnu.mnr.satellitemodeling.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellitemodeling.model.po.resources.Region;
import nnu.mnr.satellitemodeling.model.po.resources.Scene;
import nnu.mnr.satellitemodeling.model.po.resources.SceneSP;
import nnu.mnr.satellitemodeling.model.po.user.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/10 15:28
 * @Description:
 */

@FeignClient(name = "satellite-resource", contextId = "resourceClient", configuration = FeignConfig.class)
public interface ResourceClient {

    @GetMapping("/api/v1/resource/internal/region/id/{regionId}")
    Region getRegionById(@PathVariable Integer regionId);

    @GetMapping("/api/v1/resource/internal/image/dto/scene/{sceneId}")
    List<ModelServerImageDTO> getModelServerImageDTO(@PathVariable String sceneId);

    @GetMapping("/api/v1/resource/internal/scene/id/{sceneId}")
    SceneSP getSceneByIdWithProductAndSensor(@PathVariable String sceneId);

}
