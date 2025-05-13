package nnu.mnr.satelliteresource.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satelliteresource.model.po.Scene;
import nnu.mnr.satelliteresource.model.vo.resources.SceneDesVO;
import nnu.mnr.satelliteresource.utils.typeHandler.GeometryTypeHandler;
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

    @Select("SELECT sc.scene_name, sc.scene_time, sc.tile_level_num, sc.tile_levels, sc.coordinate_system, " +
            "sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, " +
            "ss.sensor_name, pd.product_name " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_time BETWEEN #{startTime} AND #{endTime} " +
            "AND sc.cloud < #{cloud} " +
            "AND ( ST_Intersects(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) OR " +
            "ST_Contains(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) OR " +
            "ST_Within(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), sc.bounding_box) )")
    List<SceneDesVO> getScenesDesByTimeCloudAndGeometry(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("cloud") Integer cloud,
            @Param("wkt") String wkt);

    @Select("SELECT sc.scene_name, sc.scene_time, sc.tile_level_num, sc.tile_levels, sc.coordinate_system, " +
            "sc.description, sc.band_num, sc.bands, sc.cloud, sc.tags, " +
            "ss.sensor_name, pd.product_name " +
            "FROM scene_table sc " +
            "LEFT JOIN sensor_table ss ON sc.sensor_id = ss.sensor_id " +
            "LEFT JOIN product_table pd ON sc.product_id = pd.product_id " +
            "WHERE sc.scene_id = #{sceneId} ")
    SceneDesVO getScenesDesById(@Param("sceneId") String sceneId);

}
