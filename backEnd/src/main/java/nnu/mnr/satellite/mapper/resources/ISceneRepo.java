package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.dto.admin.scene.CloudPathsDTO;
import nnu.mnr.satellite.model.dto.resources.SceneImageDTO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.SceneSP;
import nnu.mnr.satellite.model.vo.admin.SceneSimpleInfoVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.JSONArrayTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.SetTypeHandler;
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
            "sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data, " +
            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, ss.data_type " +
//            "(SELECT CONCAT('[', GROUP_CONCAT(JSON_OBJECT('path', im.tif_path, 'band', im.band)), ']') " +
//            "FROM image_table im WHERE im.scene_id = sc.scene_id) AS images " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_time BETWEEN #{startTime} AND #{endTime} " +
            "AND sc.cloud < #{cloud} " +
            "AND ss.data_type in (${dataType}) " +
            "AND (ST_Intersects(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box)) " +
//            "AND ( ST_Intersects(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) OR " +
//            "ST_Contains(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) OR " +
//            "ST_Within(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) )" +
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
            @Result(property = "tags", column = "tags", typeHandler = FastJson2TypeHandler.class),
            @Result(property = "noData", column = "no_data"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "platformName", column = "platform_name"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "resolution", column = "resolution"),
            @Result(property = "dataType", column = "data_type")
//            @Result(property = "images", column = "images", typeHandler = JSONArrayTypeHandler.class)
    })
    List<SceneDesVO> getScenesDesByTimeCloudAndGeometry(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("cloud") float cloud,
            @Param("wkt") String wkt,
            @Param("dataType") String dataType);

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.tile_level_num, sc.tile_levels, sc.coordinate_system, " +
            "sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data, " +
            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, " +
//            "(SELECT CONCAT('[', GROUP_CONCAT(JSON_OBJECT('path', im.tif_path, 'band', im.band)), ']') " +
//            "FROM image_table im WHERE im.scene_id = sc.scene_id) AS images " +
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
            @Result(property = "noData", column = "no_data"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "platformName", column = "platform_name"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "resolution", column = "resolution"),
//            @Result(property = "images", column = "images", typeHandler = JSONArrayTypeHandler.class)
    })
    SceneDesVO getScenesDesById(@Param("sceneId") String sceneId);

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.coordinate_system, " +
            "sc.band_num, sc.bands, sc.cloud, sc.tags, sc.bounding_box, sc.bucket, sc.cloud_path, sc.no_data, " +
            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_id = #{sceneId} ")
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "sceneName", column = "scene_name"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "platformName", column = "platform_name"),
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
            @Result(property = "noData", column = "no_data"),
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class),
    })
    SceneSP getSceneByIdWithProductAndSensor(@Param("sceneId") String sceneId);

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.coordinate_system, " +
            "sc.band_num, sc.bands, sc.cloud, sc.tags, sc.bounding_box, sc.bucket, sc.cloud_path, sc.no_data, " +
            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, " +
            "ST_Area(ST_Intersection(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box)) AS intersection_area " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE ss.sensor_name = #{sensor_name} " +
            "AND sc.scene_time BETWEEN #{start_time} AND #{end_time} " +
            "AND (ST_Intersects(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box)) " +
            "ORDER BY intersection_area DESC LIMIT 10")
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "sceneName", column = "scene_name"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "platformName", column = "platform_name"),
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
            @Result(property = "noData", column = "no_data"),
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class),
    })
    List<SceneSP> getSceneBySensorNameWithProductAndSensor(@Param("sensor_name") String sensorName,
                                                           @Param("start_time")String startTime,
                                                           @Param("end_time")String endTime,
                                                           @Param("wkt")String wkt);

    // 相交放在后端进行查询
    @Select("<script>" +
            "SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.coordinate_system, " +
            "sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data, sc.bounding_box, sc.bucket, sc.cloud_path, " +
            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, ss.data_type " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE " +
            "<choose>" +
            "    <when test='sceneIds != null and !sceneIds.isEmpty()'>" +
            "        sc.scene_id IN " +
            "        <foreach item='sceneId' collection='sceneIds' open='(' separator=',' close=')'>" +
            "            #{sceneId}" +
            "        </foreach>" +
            "    </when>" +
            "    <otherwise>" +
            "        1=0" +
            "    </otherwise>" +
            "</choose>" +
            "AND ss.data_type in ('satellite', 'dem', 'svr', 'ndvi', 'dsm', '3d') " +
            "ORDER BY sc.scene_time ASC" +
            "</script>")
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "sceneName", column = "scene_name"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "platformName", column = "platform_name"),
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
            @Result(property = "noData", column = "no_data"),
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class),
            @Result(property = "dataType", column = "data_type"),
    })
    List<SceneSP> getScenesByIdsWithProductAndSensor(@Param("sceneIds") List<String> sceneIds);

    // 数据库中求相交
//    @Select("<script>" +
//            "SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.coordinate_system, " +
//            "sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data, sc.bounding_box, sc.bucket, sc.cloud_path, " +
//            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, ss.data_type " +
//            "FROM scene_table sc " +
//            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
//            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
//            "WHERE " +
//            "<choose>" +
//            "    <when test='sceneIds != null and !sceneIds.isEmpty()'>" +
//            "        sc.scene_id IN " +
//            "        <foreach item='sceneId' collection='sceneIds' open='(' separator=',' close=')'>" +
//            "            #{sceneId}" +
//            "        </foreach>" +
//            "    </when>" +
//            "    <otherwise>" +
//            "        1=0" +
//            "    </otherwise>" +
//            "</choose>" +
//            "<if test='wkt != null and wkt != \"\"'>" +
//            "    AND ST_Contains(sc.bounding_box, ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'))" +
//            "</if>" +
//            "AND ss.data_type in ('satellite', 'dem', 'svr', 'ndvi', 'dsm', '3d') " +
//            "ORDER BY sc.scene_time ASC" +
//            "</script>")
//    @Results({
//            @Result(property = "sceneId", column = "scene_id"),
//            @Result(property = "sceneName", column = "scene_name"),
//            @Result(property = "sensorName", column = "sensor_name"),
//            @Result(property = "platformName", column = "platform_name"),
//            @Result(property = "productName", column = "product_name"),
//            @Result(property = "resolution", column = "resolution"),
//            @Result(property = "sceneTime", column = "scene_time"),
//            @Result(property = "coordinateSystem", column = "coordinate_system"),
//            @Result(property = "bandNum", column = "band_num"),
//            @Result(property = "bands", column = "bands", typeHandler = SetTypeHandler.class),
//            @Result(property = "cloud", column = "cloud"),
//            @Result(property = "bucket", column = "bucket"),
//            @Result(property = "cloudPath", column = "cloud_path"),
//            @Result(property = "tags", column = "tags", typeHandler = FastJson2TypeHandler.class),
//            @Result(property = "noData", column = "no_data"),
//            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class),
//            @Result(property = "dataType", column = "data_type"),
//    })
//    List<SceneSP> getScenesByIdsAndGridWithProductAndSensor(@Param("sceneIds") List<String> sceneIds, @Param("wkt") String wkt);

    @Select("SELECT sc.scene_id, sc.scene_name, sc.scene_time, " +
            "sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data, sc.bucket, sc.cloud_path, " +
            "ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, " +
            "(SELECT CONCAT('[', GROUP_CONCAT(JSON_OBJECT('tifPath', im.tif_path, 'band', im.band, 'bucket', im.bucket)), ']') " +
            "FROM image_table im WHERE im.scene_id = sc.scene_id) AS images " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_id = #{sceneId} ")
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "sceneName", column = "scene_name"),
            @Result(property = "sensorName", column = "sensor_name"),
            @Result(property = "productName", column = "product_name"),
            @Result(property = "platformName", column = "platform_name"),
            @Result(property = "resolution", column = "resolution"),
            @Result(property = "sceneTime", column = "scene_time"),
            @Result(property = "bandNum", column = "band_num"),
            @Result(property = "bands", column = "bands", typeHandler = SetTypeHandler.class),
            @Result(property = "cloud", column = "cloud"),
            @Result(property = "bucket", column = "bucket"),
            @Result(property = "cloudPath", column = "cloud_path"),
            @Result(property = "tags", column = "tags", typeHandler = FastJson2TypeHandler.class),
            @Result(property = "noData", column = "no_data"),
            @Result(property = "images", column = "images", typeHandler = JSONArrayTypeHandler.class)
    })
    SceneImageDTO getSceneWithImages(@Param("sceneId") String sceneId);

    @Select({
            "<script>",
            "SELECT COUNT(1) > 0 FROM scene_table WHERE sensor_id IN",
            "<foreach collection='sensorIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    boolean existsBySensorIds(@Param("sensorIds") List<String> sensorIds);

    @Select("<script>" +
            "SELECT scene_id, bucket, cloud_path " +
            "FROM scene_table " +
            "WHERE scene_id IN " +
            "<foreach collection='sceneIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<CloudPathsDTO> findFilePathsBySceneIds(@Param("sceneIds") List<String> sceneIds);
}
