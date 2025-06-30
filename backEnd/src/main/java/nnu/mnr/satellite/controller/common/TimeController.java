package nnu.mnr.satellite.controller.common;

import nnu.mnr.satellite.model.dto.modeling.NoCloudFetchDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.modeling.ModelExampleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @name: TimeController
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/25/2025 9:12 PM
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/v1/time")
public class TimeController {

    @GetMapping("/current")
    public ResponseEntity<LocalDateTime> getNoCloudByRegion(@RequestBody NoCloudFetchDTO noCloudFetchDTO) throws IOException {
        return ResponseEntity.ok(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    }
}
