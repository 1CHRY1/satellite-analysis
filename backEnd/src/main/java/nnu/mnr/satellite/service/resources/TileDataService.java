package nnu.mnr.satellite.service.resources;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Tile;
import nnu.mnr.satellite.repository.ITileRepo;
import nnu.mnr.satellite.utils.MinioUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    MinioUtil minioUtil;

    private final ITileRepo tileRepo;

    public TileDataService(ITileRepo tileRepo) {
        this.tileRepo = tileRepo;
    }

    public List<Tile> getTilesByImageAndLevel(String imageId, int tileLevel) {
        QueryWrapper<Tile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("image_id", imageId);
        queryWrapper.eq("tile_level", tileLevel);
        return tileRepo.selectList(queryWrapper);
    }

    public byte[] getTileTifById(String tileId) {
        QueryWrapper<Tile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tileId", tileId);
        Tile tile = tileRepo.selectOne(queryWrapper);
        return minioUtil.downloadByte(tile.getBucket(), tile.getPath());
    }

}
