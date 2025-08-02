package nnu.mnr.satellite.mapper.resources;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Vector;
import nnu.mnr.satellite.model.vo.resources.VectorInfoVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

@DS("pg_satellite")
public interface IVectorRepo  extends BaseMapper<Vector> {
    @Select("SELECT vector_name, table_name, time FROM gis_db.vector_datasets " +
//            "WHERE ST_Intersects(ST_GeomFromEWKT('SRID=4326;' || #{wkt}), gis_db.vector_datasets.boundary) " +
            "ORDER BY time ASC")
    @Results({
            @Result(property = "vectorName", column = "vector_name"),
            @Result(property = "tableName", column = "table_name"),
            @Result(property = "time", column = "time"),
    })
    List<VectorInfoVO> getVectorsDesByTimeAndGeometry(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("wkt") String wkt
    );

    @Select(
            "WITH mvt_geom AS (" +
                    "   SELECT " +
                    "       ST_AsMVTGeom(" +
                    "           ST_Transform(original_table.geom, 3857), " + // 先转换到 Web Mercator
                    "           ST_Transform(ST_TileEnvelope(#{z}, #{x}, #{y}), 3857), " + // tile 边界也转换到 3857
                    "           extent => 256, " + // 典型 MVT 范围
                    "           buffer => 64, " + // 调整缓冲区
                    "           clip_geom => true " + // 确保裁剪
                    "       ) AS geom " +
                    "   FROM gis_db.${tableName} AS original_table " +
                    "   WHERE ST_Intersects(" +
                    "       ST_Transform(original_table.geom, 4326), " + // 保持过滤条件在 4326
                    "       ST_GeomFromText(#{wkt}, 4326)" +
                    "   )" +
                    ")" +
                    "SELECT ST_AsMVT(t, '${tableName}', 256, 'geom') AS mvt " +
                    "FROM mvt_geom AS t " +
                    "WHERE geom IS NOT NULL" // 过滤掉可能被裁剪掉的空几何
    )
    Object getVectorByTableNameAndGeometry(
            @Param("tableName") String tableName,
            @Param("wkt") String wkt,
            @Param("z") int z,
            @Param("x") int x,
            @Param("y") int y
    );

    @Select({
            "<script>",
            "SELECT vector_name, table_name, time",
            "FROM gis_db.vector_datasets",
            "WHERE 1=1",
            // 动态过滤 tableNames（如果传入）
            "<if test='tableNames != null and tableNames.size() > 0'>",
            "   AND table_name IN ",
            "   <foreach collection='tableNames' item='tableName' open='(' separator=',' close=')'>",
            "       #{tableName}",
            "   </foreach>",
            "</if>",
            // 空间相交判定
//            "   AND ST_Intersects(boundary,  ST_SetSRID(ST_GeomFromText(#{gridPolyText}), 4326))",
            "</script>"
    })
    List<VectorInfoVO> findIntersectingVectors(@Param("gridPolyText") String gridPolyText, @Param("tableNames") List<String> tableNames);
}
