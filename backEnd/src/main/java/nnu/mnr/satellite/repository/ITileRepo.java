package nnu.mnr.satellite.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.model.po.Sensor;
import nnu.mnr.satellite.model.po.Tile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

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

    @Select("select bounding_box from ${tileTable} where tile_level = #{tileLevel}")
    List<Tile> getTileByImageIdAndLevel(@Param("tileTable") String tileTable, int tileLevel);

    @Select("select * from ${tileTable} where tile_id = #{tileId}")
    Tile getTileByTileId(@Param("tileTable") String tileTable, String tileId);

}
