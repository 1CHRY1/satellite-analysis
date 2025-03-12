package nnu.mnr.satellite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Tile;
import nnu.mnr.satellite.repository.ITileRepo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:28
 * @Description:
 */

@Service("TileDataService")
public class TileDataService {

    private final ITileRepo tileRepo;

    public TileDataService(ITileRepo tileRepo) {
        this.tileRepo = tileRepo;
    }

    public List<Tile> getTileByImageAndLevel(String imageId, int tileLevel) {
        QueryWrapper<Tile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("image_id", imageId);
        queryWrapper.eq("tile_level", tileLevel);
        return tileRepo.selectList(queryWrapper);
    }

}
