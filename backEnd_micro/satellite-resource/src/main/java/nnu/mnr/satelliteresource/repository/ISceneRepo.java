package nnu.mnr.satelliteresource.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satelliteresource.model.po.Scene;
import nnu.mnr.satelliteresource.model.po.SceneSP;
import nnu.mnr.satelliteresource.model.vo.resources.SceneDesVO;
import nnu.mnr.satelliteresource.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satelliteresource.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satelliteresource.utils.typeHandler.SetTypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:26
 * @Description:
 */

//@Repository("ImageRepo")
public interface ISceneRepo extends BaseMapper<Scene> {

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.tile_level_num, sc.tile_levels, sc.coordinate_system, " +
            "sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, " +
            "ss.sensor_name, pd.product_name, pd.resolution, " +
            "(SELECT CONCAT('[', GROUP_CONCAT(JSON_OBJECT('path', im.tif_path, 'band', im.band)), ']') " +
            "FROM image_table im WHERE im.scene_id = sc.scene_id) AS image_list " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_time BETWEEN #{startTime} AND #{endTime} " +
            "AND sc.cloud < #{cloud} " +
            "AND ( ST_Intersects(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) OR " +
            "ST_Contains(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) OR " +
            "ST_Within(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) )")
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
            @Result(property = "tags", column = "tags", typeHandler = FastJson2TypeHandler.class),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "resolution", column = "resolution"),
//            @Result(property = "imageList", column = "image_list", typeHandler = JSONArrayTypeHandler.class)
    })
    List<SceneDesVO> getScenesDesByTimeCloudAndGeometry(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("cloud") Integer cloud,
            @Param("wkt") String wkt);

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.tile_level_num, sc.tile_levels, sc.coordinate_system, " +
            "sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, " +
            "ss.sensor_name, pd.product_name, pd.resolution, " +
            "(SELECT CONCAT('[', GROUP_CONCAT(JSON_OBJECT('path', im.tif_path, 'band', im.band)), ']') " +
            "FROM image_table im WHERE im.scene_id = sc.scene_id) AS image_list " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_id = #{sceneId} ")
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
            @Result(property = "tags", column = "tags", typeHandler = FastJson2TypeHandler.class),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "resolution", column = "resolution"),
//            @Result(property = "imageList", column = "image_list", typeHandler = JSONArrayTypeHandler.class)
    })
    SceneDesVO getScenesDesById(@Param("sceneId") String sceneId);

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.coordinate_system, " +
            "sc.band_num, sc.bands, sc.cloud, sc.tags, sc.bounding_box, sc.bucket, sc.cloud_path, " +
            "ss.sensor_name, pd.product_name, pd.resolution " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_id = #{sceneId} ")
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "sceneName", column = "scene_name"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "resolution", column = "resolution"),
            @Result(property = "sceneTime", column = "scene_time"),
            @Result(property = "coordinateSystem", column = "coordinate_system"),
            @Result(property = "bandNum", column = "band_num"),
            @Result(property = "bands", column = "bands", typeHandler = SetTypeHandler.class),
            @Result(property = "cloud", column = "cloud"),
            @Result(property = "bucket", column = "bucket"),
            @Result(property = "cloudPath", column = "cloud_path"),
            @Result(property = "tags", column = "tags", typeHandler = FastJson2TypeHandler.class),
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class),
    })
    SceneSP getSceneByIdWithProductAndSensor(@Param("sceneId") String sceneId);

}
