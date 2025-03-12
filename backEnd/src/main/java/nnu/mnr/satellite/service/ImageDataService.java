package nnu.mnr.satellite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.repository.IImageRepo;
import org.springframework.stereotype.Service;

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

    private final IImageRepo imageRepo;

    public ImageDataService(IImageRepo imageRepo) {
        this.imageRepo = imageRepo;
    }

    public List<Image> getImageByProductId(String productId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        return imageRepo.selectList(queryWrapper);
    }

}
