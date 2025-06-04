package nnu.mnr.satellite.service.resources;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.vo.resources.SensorDesVO;
import nnu.mnr.satellite.model.vo.resources.SensorInfoVO;
import nnu.mnr.satellite.model.po.resources.Sensor;
import nnu.mnr.satellite.mapper.resources.ISensorRepo;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    @Autowired
    private ModelMapper sensorModelMapper;

    private final ISensorRepo sensorRepo;

    public SensorDataService(ISensorRepo sensorRepo) {
        this.sensorRepo = sensorRepo;
    }

    public List<SensorInfoVO> getAllData() {
        QueryWrapper<Sensor> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sensor_id", "sensor_name", "platform_name");
        List<Sensor> sensors = sensorRepo.selectList(queryWrapper);
        if (sensors == null) {
            return Collections.emptyList();
        }
        return sensorModelMapper.map(sensors, new TypeToken<List<SensorInfoVO>>() {}.getType());
    }

    public SensorDesVO getSensorDescription(String sensorId) {
        QueryWrapper<Sensor> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("description").eq("sensor_id", sensorId);
        Sensor sensor = sensorRepo.selectOne(queryWrapper);
        if (sensor == null) {
            return null;
        }
        return sensorModelMapper.map(sensor, new TypeToken<SensorDesVO>() {}.getType());
    }

}
