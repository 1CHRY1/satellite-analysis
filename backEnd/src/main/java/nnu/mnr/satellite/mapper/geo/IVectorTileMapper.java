package nnu.mnr.satellite.mapper.geo;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.pojo.common.TileBox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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

    Object getVectorTile(TileBox tileBox);

}
