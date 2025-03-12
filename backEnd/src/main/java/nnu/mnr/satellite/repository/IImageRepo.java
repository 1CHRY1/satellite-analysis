package nnu.mnr.satellite.repository;

import nnu.mnr.satellite.model.po.Image;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:26
 * @Description:
 */

//@Repository("ImageRepo")
@Mapper
public interface IImageRepo {
    List<Image> getImageByProductId(String productId);
}
