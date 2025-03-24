package nnu.mnr.satellite.repository.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Tile;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:27
 * @Description:
 */

//@Repository("tileRepo")
public interface ITileRepoV2 extends BaseMapper<Tile> {

    @Select("select tile_id, bounding_box from ${tileTable} where tile_level = #{tileLevel}")
    @Results({
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class)
    })
    List<Tile> getTileByImageIdAndLevel(@Param("tileTable") String tileTable, int tileLevel);

    @Select("select scene_id, image_id, tile_level, cloud, band, column_id, row_id, bucket, path from ${tileTable} where tile_id = #{tileId}")
    Tile getTileByTileId(@Param("tileTable") String tileTable, String tileId);

}
