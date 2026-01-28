package nnu.mnr.satellite.mapper.resources;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.dto.resources.MinMaxResult;
import nnu.mnr.satellite.model.po.resources.Vector;
import nnu.mnr.satellite.model.vo.resources.VectorInfoVO;
import nnu.mnr.satellite.model.vo.resources.VectorTypeVO;
import org.apache.ibatis.annotations.*;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@DS("pg-satellite")
public interface IVectorRepo  extends BaseMapper<Vector> {
    @Select("SELECT vector_name, table_name, time FROM gis_db.vector_datasets " +
            "WHERE ST_Intersects(ST_GeomFromEWKT('SRID=4326;' || #{wkt}), gis_db.vector_datasets.boundary) " +
            "ORDER BY time ASC")
    @Results({
            @Result(property = "vectorName", column = "vector_name"),
            @Result(property = "tableName", column = "table_name"),
            @Result(property = "time", column = "time"),
            @Result(property = "fields.discrete", column = "table_name",
                    many = @Many(select = "nnu.mnr.satellite.mapper.resources.IVectorRepo.getDiscreteFields")),
            @Result(property = "fields.continuous", column = "table_name",
                    many = @Many(select = "nnu.mnr.satellite.mapper.resources.IVectorRepo.getContinuousFields"))
    })
    List<VectorInfoVO> getVectorsDesByTimeAndGeometry(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
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
            "SELECT DISTINCT ${field} ",
            "FROM gis_db.${tableName} ",
            "WHERE ST_Intersects(geom, ST_GeomFromText(#{wkt}, 4326))"  // 假设 WKT 是 WGS84 坐标系 (SRID=4326)
    })
    List<String> getVectorTypeByTableNameAndField(@Param("tableName") String tableName, @Param("field") String field, @Param("wkt") String wkt);

    @Select({
            "SELECT MIN(${field}) as min_val, MAX(${field}) as max_val ",
            "FROM gis_db.${tableName} ",
            "WHERE ST_Intersects(geom, ST_GeomFromText(#{wkt}, 4326))"
    })
    @Results({
            @Result(property = "min", column = "min_val"),
            @Result(property = "max", column = "max_val")
    })
    MinMaxResult getMinMaxByTableNameAndFieldAndCount(@Param("tableName") String tableName, @Param("field") String field, @Param("wkt") String wkt);

    // 查询指定表的字段名（排除 id、fid、geom）
    @Select("SELECT column_name " +
            "FROM information_schema.columns " +
            "WHERE table_schema = 'gis_db' " +
            "AND table_name = #{tableName} " +
            "AND column_name NOT LIKE '%id%' " +
            "AND column_name != 'geom' "
            )
    List<String> getTableFields(@Param("tableName") String tableName);

    @Select("SELECT column_name " +
            "FROM information_schema.columns " +
            "WHERE table_schema = 'gis_db' " +
            "AND table_name = #{tableName} " +
            "AND column_name NOT LIKE '%id%' " +
            "AND column_name != 'geom' " +
            "AND data_type NOT IN ('float', 'double precision', 'real', 'numeric', 'decimal') "
            )
    List<String> getDiscreteFields(@Param("tableName") String tableName, @Param("columnName") String columnName);

    @Select("SELECT column_name " +
            "FROM information_schema.columns " +
            "WHERE table_schema = 'gis_db' " +
            "AND table_name = #{tableName} " +
            "AND column_name NOT LIKE '%id%' " +
            "AND column_name != 'geom' " +
            "AND data_type IN ('float', 'double precision', 'real', 'numeric', 'decimal') "
    )
    List<String> getContinuousFields(@Param("tableName") String tableName, @Param("columnName") String columnName);
}
