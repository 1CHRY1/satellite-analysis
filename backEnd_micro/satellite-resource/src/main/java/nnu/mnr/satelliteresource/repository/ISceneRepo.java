package nnu.mnr.satelliteresource.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satelliteresource.model.po.Scene;
import nnu.mnr.satelliteresource.utils.typeHandler.GeometryTypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:26
 * @Description:
 */

//@Repository("ImageRepo")
public interface ISceneRepo extends BaseMapper<Scene> {

    @Select("SELECT scene_time, tile_level_num, tile_levels," +
            "scene_name, coordinate_system," +
            "description, band_num, bands, cloud FROM scene_table WHERE scene_id = #{sceneId}")
    @Results({
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class)
    })
    Scene getSceneById(@Param("sceneId") String sceneId);
}
