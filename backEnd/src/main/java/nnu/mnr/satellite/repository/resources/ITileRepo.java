package nnu.mnr.satellite.repository.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.dto.resources.TileBasicDTO;
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
public interface ITileRepo extends BaseMapper<Tile> {

    @Select("select tile_id, bounding_box from ${tileTable} where tile_level = #{tileLevel} and band = #{band}")
    @Results({
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class)
    })
    List<Tile> getTileByBandAndLevel(@Param("tileTable") String tileTable, String band, String tileLevel);

    @Select("select distinct row_id, column_id from ${tileTable} where tile_level = #{tileLevel} and " +
            "( ST_Intersects(ST_GeomFromText(#{wkt}, 4326), bounding_box) OR " +
            "ST_Contains(ST_GeomFromText(#{wkt}, 4326), bounding_box) OR " +
            "ST_Within(ST_GeomFromText(#{wkt}, 4326), bounding_box) ) ")
    List<TileBasicDTO> getBasicTileByBandAndLevel(@Param("tileTable") String tileTable, String tileLevel, String wkt);

    @Select("select scene_id, image_id, tile_level, cloud, band, column_id, row_id, bucket, path from ${tileTable} where tile_id = #{tileId}")
    Tile getTileByTileId(@Param("tileTable") String tileTable, String tileId);

}
