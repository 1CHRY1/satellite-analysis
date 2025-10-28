package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Sensor;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:22
 * @Description:
 */

//@Repository("SensorRepo")
public interface ISensorRepo extends BaseMapper<Sensor> {
    @Select("SELECT COUNT(1) > 0 FROM sensor_table WHERE sensor_name = #{sensorName}")
    boolean existsBySensorName(@Param("sensorName") String sensorName);

    @Select("SELECT * FROM sensor_table WHERE sensor_name = #{sensorName}")
    Sensor selectBySensorName(@Param("sensorName") String sensorName);

    @Select("SELECT COUNT(1) > 0 FROM sensor_table WHERE sensor_id = #{sensorId}")
    boolean existsBySensorId(@Param("sensorId") String sensorId);
}
