package nnu.mnr.satellite.repository.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Product;

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
