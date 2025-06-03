package nnu.mnr.satellite.mapper.geo;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.stream.Stream;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/27 11:14
 * @Description:
 */

@Mapper
@DS("pg_space")
public interface IVectorTileMapper extends BaseMapper<Object> {

    Object getVectorTile(String tableName, int x, int y, int z);

    Object getVectorTileByBetweenParam(String tableName, int x, int y, int z, String param, Integer valueStart, Integer valueEnd, String wkt);

}
