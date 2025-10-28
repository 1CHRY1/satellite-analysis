package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.sensor.SensorDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.sensor.SensorInsertDTO;
import nnu.mnr.satellite.model.dto.admin.sensor.SensorPageDTO;
import nnu.mnr.satellite.model.dto.admin.sensor.SensorUpdateDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminSensorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/sensor")
public class AdminSensorController {

    @Autowired
    private AdminSensorService adminSensorService;

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getSensorInfoPage(@RequestBody SensorPageDTO sensorPageDTO) throws Exception {
        return ResponseEntity.ok(adminSensorService.getSensorInfoPage(sensorPageDTO));
    }

    @PutMapping("/insert")
    public ResponseEntity<CommonResultVO> insertSensor(@RequestBody SensorInsertDTO sensorInsertDTO) throws Exception {
        return ResponseEntity.ok(adminSensorService.insertSensor(sensorInsertDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateSensor(@RequestBody SensorUpdateDTO sensorUpdateDTO) throws Exception {
        return ResponseEntity.ok(adminSensorService.updateSensor(sensorUpdateDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteSensor(@RequestBody SensorDeleteDTO sensorDeleteDTO) throws Exception {
        return ResponseEntity.ok(adminSensorService.deleteSensor(sensorDeleteDTO));
    }
}
