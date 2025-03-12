package nnu.mnr.satellite.service;

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
        return sensorRepo.selectList(null);
    }

}
