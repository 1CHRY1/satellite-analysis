package nnu.mnr.satellite.controller.resources;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.vo.resources.SensorDesVO;
import nnu.mnr.satellite.model.vo.resources.SensorInfoVO;
import nnu.mnr.satellite.service.resources.SensorDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 21:30
 * @Description:
 */

@RestController
@RequestMapping("api/v1/data/sensor")
@Slf4j
public class SensorController {

    private final SensorDataService sensorDataService;

    public SensorController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @GetMapping
    public ResponseEntity<List<SensorInfoVO>> GetAllData() {
        return ResponseEntity.ok(sensorDataService.getAllData());
    }

    @GetMapping("/description/sensorId/{sensorId}")
    public ResponseEntity<SensorDesVO> GetSensorDescription(@PathVariable String sensorId) {
        return ResponseEntity.ok(sensorDataService.getSensorDescription(sensorId));
    }

}
