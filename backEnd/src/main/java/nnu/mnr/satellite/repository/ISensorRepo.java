package nnu.mnr.satellite.repository;

import nnu.mnr.satellite.model.po.Sensor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:22
 * @Description:
 */

//@Repository("SensorRepo")
@Mapper
public interface ISensorRepo {
    List<Sensor> getAllSensorData();
}
