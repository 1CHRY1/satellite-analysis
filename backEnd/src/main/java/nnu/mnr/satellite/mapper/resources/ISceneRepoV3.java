package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.vo.admin.SceneSimpleInfoVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.PostgisGeometryTypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface ISceneRepoV3 extends BaseMapper<Scene> {

    @Select({"<script>",
            "WITH filter_geom AS (",
            "  SELECT ST_GeomFromText(#{wkt}, 4326) AS geom",
            ")",
            "SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.tile_level_num, sc.tile_levels, sc.coordinate_system,",
            "       sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data,",
            "       ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, ss.data_type, sc.bounding_box,",
            "       sc.cloud_path, sc.bucket",
            "FROM scene_table sc",
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id",
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id",
            "CROSS JOIN filter_geom fg",
            "<where>",
            "  <choose>",
            "    <when test='dataTypes.contains(\"satellite\")'>",
            "      (ss.data_type = 'satellite' AND sc.scene_time BETWEEN #{startTime} AND #{endTime})",
            "      <if test='dataTypes.size() > 1'>",
            "        OR (ss.data_type != 'satellite' AND ss.data_type in",
            "        <foreach item='dt' collection='dataTypes' separator=',' open='(' close=')'>",
            "          #{dt}",
            "        </foreach>)",
            "      </if>",
            "    </when>",
            "    <otherwise>",
            "      ss.data_type in",
            "      <foreach item='dt' collection='dataTypes' separator=',' open='(' close=')'>",
            "        #{dt}",
            "      </foreach>",
            "    </otherwise>",
            "  </choose>",
            "  AND sc.bounding_box &amp;&amp; fg.geom",
            "  AND ST_Intersects(sc.bounding_box, fg.geom)",
            "</where>",
            "ORDER BY sc.scene_time ASC",
            "</script>"})
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
            @Result(property = "boundingBox", column = "bounding_box", typeHandler = PostgisGeometryTypeHandler.class),
    })
    List<SceneDesVO> getScenesInfoByTimeAndRegion(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("wkt") String wkt,
            @Param("dataTypes") List<String> dataType);

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
