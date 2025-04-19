package nnu.mnr.satelliteresource.repository;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satelliteresource.model.po.Tile;
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
 * @Date: 2025/3/11 17:27
 * @Description:
 */

//@Repository("tileRepo")
public interface ITileRepo extends BaseMapper<Tile> {

    @Select("select tile_id, column_id, row_id, bounding_box from ${tileTable} where tile_level = #{tileLevel} and band = #{band}")
    @Results({
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class)
    })
    @DS("mysql_tile")
    List<Tile> getTileByBandAndLevel(@Param("tileTable") String tileTable, String band, String tileLevel);

    @Select("select tile_id, bucket, path, cloud from ${tileTable} where tile_level = #{tileLevel} and band = #{band} and row_id = #{rowId} and column_id = #{columnId}")
    @Results({
            @Result(property = "bbox", column = "bounding_box", typeHandler = GeometryTypeHandler.class)
    })
    @DS("mysql_tile")
    Tile getTileDataByBandLevelAndIds(@Param("tileTable") String tileTable, String band, String tileLevel, Integer rowId, Integer columnId);

//    @Select("select distinct row_id, column_id from ${tileTable} where tile_level = #{tileLevel} and " +
//            "( ST_Intersects(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
//            "ST_Contains(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), bounding_box) OR " +
//            "ST_Within(ST_GeomFromText(#{wkt}, 4326, 'axis-order=long-lat'), bounding_box) ) ")
//    @DS("mysql_tile")
//    Geometry getGeometryByColumnId(@Param("tileTable") String tileTable, String tileLevel, String wkt);

    @Select("select image_id, tile_level, cloud, band, column_id, row_id, bucket, path from ${tileTable} where tile_id = #{tileId}")
    @DS("mysql_tile")
    Tile getTileByTileId(@Param("tileTable") String tileTable, String tileId);

}
