package nnu.mnr.satellite.repository;

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
@Mapper
public interface ITileRepo {
    List<Tile> getTileByImageAndLevel(String imageId, int tileLevel);
}
