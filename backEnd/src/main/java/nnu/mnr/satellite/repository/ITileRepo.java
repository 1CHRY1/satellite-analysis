package nnu.mnr.satellite.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.model.po.Sensor;
import nnu.mnr.satellite.model.po.Tile;
import org.apache.ibatis.annotations.Mapper;
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
//    List<Tile> getTileByImageAndLevel(String imageId, int tileLevel);
}
