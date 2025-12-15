package nnu.mnr.satellite.mapper.resources;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Vector;
import nnu.mnr.satellite.model.vo.resources.VectorInfoVO;
import nnu.mnr.satellite.model.vo.resources.VectorTypeVO;
import org.apache.ibatis.annotations.*;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

@Mapper
@DS("pg-satellite")
public interface IVectorRepo  extends BaseMapper<Vector> {
    @Select("SELECT vector_name, table_name, time FROM gis_db.vector_datasets " +
//            "WHERE ST_Intersects(ST_GeomFromEWKT('SRID=4326;' || #{wkt}), gis_db.vector_datasets.boundary) " +
            "ORDER BY time ASC")
    @Results({
            @Result(property = "vectorName", column = "vector_name"),
            @Result(property = "tableName", column = "table_name"),
            @Result(property = "time", column = "time"),
            @Result(property = "fields", column = "table_name",
                    many = @Many(select = "nnu.mnr.satellite.mapper.resources.IVectorRepo.getTableFields"))
    })
    List<VectorInfoVO> getVectorsDesByTimeAndGeometry(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("wkt") String wkt
    );

    Object getVectorByTableNameAndGeometry(
            @Param("tableName") String tableName,
            @Param("wkt") String wkt,
            @Param("field") String field,
            @Param("z") int z,
            @Param("x") int x,
            @Param("y") int y,
            @Param("columns") List<String> columns,
            @Param("types") List<String> types
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

    @Select({
            "SELECT DISTINCT type, label FROM gis_db.${tableName}"
    })
    List<VectorTypeVO> getVectorTypeByTableName(@Param("tableName") String tableName);

    @Select({
            "SELECT DISTINCT ${field} FROM gis_db.${tableName}"
    })
    List<String> getVectorTypeByTableNameAndField(@Param("tableName") String tableName, @Param("field") String field);

    // 查询指定表的字段名（排除 id、fid、geom）
    @Select("SELECT column_name " +
            "FROM information_schema.columns " +
            "WHERE table_schema = 'gis_db' " +  // 替换为你的数据库 schema 名
            "AND table_name = #{tableName} " +
            "AND column_name NOT IN ('id', 'fid', 'geom')")
    List<String> getTableFields(@Param("tableName") String tableName);
}
