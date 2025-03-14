package nnu.mnr.satellite.service.resources;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Sensor;
import nnu.mnr.satellite.repository.ISensorRepo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 21:29
 * @Description:
 */

@Service("SensorDataService")
public class SensorDataService {

    private final ISensorRepo sensorRepo;

    public SensorDataService(ISensorRepo sensorRepo) {
        this.sensorRepo = sensorRepo;
    }

    public List<Sensor> getAllData() {
        QueryWrapper<Sensor> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sensor_id", "sensor_name", "platform_name");
        return sensorRepo.selectList(queryWrapper);
    }

    public String getSensorDescription(String sensorId) {
        QueryWrapper<Sensor> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("description").eq("sensor_id", sensorId);
        Sensor sensor = sensorRepo.selectOne(queryWrapper);
        return sensor != null ? sensor.getDescription() : null;
    }

}
