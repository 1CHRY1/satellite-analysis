package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.vo.admin.SceneSimpleInfoVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ISceneRepoV3 extends BaseMapper<Scene> {

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.tile_level_num, sc.tile_levels, sc.coordinate_system, " +
            "sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data, " +
            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, ss.data_type, sc.bounding_box, " +
            "sc.cloud_path, sc.bucket " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE ((sc.scene_time BETWEEN #{startTime} AND #{endTime} " +
            "AND ss.data_type = 'satellite') " +
            "OR ss.data_type in (${dataType})) " +
            "AND (ST_Intersects(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box)) " +
            "ORDER BY sc.scene_time ASC")
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "sceneName", column = "scene_name"),
            @Result(property = "sceneTime", column = "scene_time"),
            @Result(property = "tileLevelNum", column = "tile_level_num"),
            @Result(property = "tileLevels", column = "tile_levels"),
            @Result(property = "coordinateSystem", column = "coordinate_system"),
            @Result(property = "description", column = "description"),
            @Result(property = "bandNum", column = "band_num"),
            @Result(property = "bands", column = "bands"),
            @Result(property = "cloud", column = "cloud"),
            @Result(property = "cloudPath", column = "cloud_path"),
            @Result(property = "bucket", column = "bucket"),
            @Result(property = "tags", column = "tags", typeHandler = FastJson2TypeHandler.class),
            @Result(property = "noData", column = "no_data"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "platformName", column = "platform_name"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "resolution", column = "resolution"),
            @Result(property = "dataType", column = "data_type"),
            @Result(property = "boundingBox", column = "bounding_box"),
    })
    List<SceneDesVO> getScenesInfoByTimeAndRegion(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("wkt") String wkt,
            @Param("dataType") String dataType);

    @Select("SELECT sc.scene_id, pd.resolution, ss.data_type " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id ")
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "resolution", column = "resolution"),
            @Result(property = "dataType", column = "data_type"),
    })
    List<SceneSimpleInfoVO> getAllScenes();
}
