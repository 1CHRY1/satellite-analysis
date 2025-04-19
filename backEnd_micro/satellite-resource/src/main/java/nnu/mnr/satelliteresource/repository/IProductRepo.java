package nnu.mnr.satelliteresource.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satelliteresource.model.po.Product;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:25
 * @Description:
 */

//@Repository("ProductRepo")
public interface IProductRepo extends BaseMapper<Product>  {
//    List<Product> getProductBySensorId(String sensorId);
}
