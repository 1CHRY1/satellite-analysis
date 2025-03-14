package nnu.mnr.satellite.service.resources;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.repository.IImageRepo;
import nnu.mnr.satellite.utils.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:20
 * @Description:
 */

@Service("ImageDataService")
public class ImageDataService {

    @Autowired
    MinioUtil minioUtil;

    private final IImageRepo imageRepo;

    public ImageDataService(IImageRepo imageRepo) {
        this.imageRepo = imageRepo;
    }

    public List<Image> getImagesBySceneId(String sceneId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("image_id", "band").eq("scene_id", sceneId);
        return imageRepo.selectList(queryWrapper);
    }

    public byte[] getTifByImageId(String imageId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("image_id", imageId);
        Image image = imageRepo.selectOne(queryWrapper);
        return minioUtil.downloadByte(image.getBucket(), image.getTifPath());
    }

}
