package nnu.mnr.satellite.repository;

import nnu.mnr.satellite.model.po.Product;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:25
 * @Description:
 */

//@Repository("ProductRepo")
@Mapper
public interface IProductRepo {
    List<Product> getProductBySensorId(String sensorId);
}
