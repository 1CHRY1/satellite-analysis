package nnu.mnr.satellite.service.resources;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.po.Product;
import nnu.mnr.satellite.repository.IProductRepo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 22:40
 * @Description:
 */

@Service("ProductDataService")
public class ProductDataService {
    private final IProductRepo productRepo;

    public ProductDataService(IProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    public List<Product> getProductBySensorId(String sensorId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("product_id", "product_name")
                .eq("sensor_id", sensorId);
        return productRepo.selectList(queryWrapper);
    }

    public List<Product> getProductDescription(String productId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("resolution", "period", "description").eq("product_id", productId);
        return productRepo.selectList(queryWrapper);
    }
}
