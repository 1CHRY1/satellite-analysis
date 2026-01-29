package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.vo.admin.SceneSimpleInfoVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;
import nnu.mnr.satellite.utils.typeHandler.PostgisGeometryTypeHandler;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Transactional
public interface ISceneRepoV3 extends BaseMapper<Scene> {


    @Update({
            "<script>",
            "DROP TABLE IF EXISTS temp_scene;",
            "CREATE TEMP TABLE temp_scene AS",
            "WITH filter_geom AS (",
            "  SELECT ST_GeomFromText(#{wkt}, 4326) AS geom",
            ")",
            "SELECT sc.scene_id, sc.scene_name, sc.scene_time, sc.sensor_id, sc.product_id, ",
            "       sc.band_num, sc.bands, sc.cloud, sc.tags, sc.no_data, ",
            "       ss.sensor_name, ss.platform_name, pd.product_name, pd.resolution, ss.data_type, sc.bounding_box, ",
            "       sc.cloud_path, sc.bucket ",
            "FROM scene_table sc",
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id",
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id",
            "CROSS JOIN filter_geom fg",
            "<where>",
            "  <if test='dataTypes.contains(\"satellite\")'>",
            "    (",
            "      (ss.data_type = 'satellite' AND sc.scene_time BETWEEN #{startTime} AND #{endTime})",
            "      <if test='dataTypes.size() > 1'>",
            "        OR (ss.data_type != 'satellite' AND ss.data_type in",
            "          <foreach item='dt' collection='dataTypes' separator=',' open='(' close=')'>",
            "            #{dt}",
            "          </foreach>)",
            "      </if>",
            "    )",
            "  </if>",
            "  <if test='!dataTypes.contains(\"satellite\")'>",
            "    ss.data_type in",
            "      <foreach item='dt' collection='dataTypes' separator=',' open='(' close=')'>",
            "        #{dt}",
            "      </foreach>",
            "  </if>",
            "  AND sc.bounding_box &amp;&amp; fg.geom",
            "  AND ST_Intersects(sc.bounding_box, fg.geom)",
            "</where>",
            ";",
            "CREATE INDEX idx_temp_scene_product_id",
            "ON temp_scene (product_id);",
            "CREATE INDEX idx_temp_scene_data_type",
            "ON temp_scene (data_type);",
            "CREATE INDEX IF NOT EXISTS idx_temp_scene_bbox",
            "ON temp_scene USING GIST (bounding_box);",
            "</script>"
    })
    void buildTempSceneTable(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("wkt") String wkt,
            @Param("dataTypes") List<String> dataType);


    @Select({"<script>",
            "SELECT * ",
            "FROM temp_scene ",
            "ORDER BY scene_time ASC",
            "</script>"})
    @Results({
            @Result(property = "sceneId", column = "scene_id"),
            @Result(property = "sceneName", column = "scene_name"),
            @Result(property = "sceneTime", column = "scene_time"),
            @Result(property = "sensorId", column = "sensor_id"),
            @Result(property = "productId", column = "product_id"),
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
    List<SceneDesVO> getScenesInfoByTimeAndRegion();

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

    @Select({
            "<script>",
            "WITH params AS (",
            "    SELECT ST_SetSRID(ST_GeomFromText(#{wkt}), 4326) AS geom",
            "),",
            "filtered AS (",
            "    SELECT bounding_box",
            "    FROM temp_scene",
            "    <if test='productIdList != null and productIdList.size() > 0'>",
            "        WHERE product_id IN",
            "        <foreach collection='productIdList' item='id' open='(' separator=',' close=')'>",
            "            #{id}",
            "        </foreach>",
            "    </if>",
            "),",
            "unioned AS (",
            "    SELECT ST_UnaryUnion(ST_Collect(bounding_box)) AS scene_union",
            "    FROM filtered",
            ")",
            "SELECT",
            "    ST_Area(ST_Intersection(scene_union, (SELECT geom FROM params))::geography) /",
            "    ST_Area((SELECT geom FROM params)::geography) AS coverage_ratio",
            "FROM unioned",
            "</script>"
    })
    Double getCoverageRatio(@Param("wkt") String wkt,
                            @Param("productIdList") List<String> productIdList);

    @Select({
            "<script>",
            "WITH params AS (",
            "    SELECT ST_SetSRID(ST_GeomFromText(#{wkt}), 4326) AS geom",
            "),",
            "filtered AS (",
            "    SELECT bounding_box",
            "    FROM temp_scene WHERE data_type = 'satellite'",
            "),",
            "unioned AS (",
            "    SELECT ST_UnaryUnion(ST_Collect(bounding_box)) AS scene_union",
            "    FROM filtered",
            ")",
            "SELECT",
            "    ST_Area(ST_Intersection(scene_union, (SELECT geom FROM params))::geography) /",
            "    ST_Area((SELECT geom FROM params)::geography) AS coverage_ratio",
            "FROM unioned",
            "</script>"
    })
    Double getAllCoverageRatio(@Param("wkt") String wkt);
}
