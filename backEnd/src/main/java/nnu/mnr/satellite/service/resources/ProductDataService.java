package nnu.mnr.satellite.service.resources;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.vo.resources.ProductDesVO;
import nnu.mnr.satellite.model.vo.resources.ProductInfoVO;
import nnu.mnr.satellite.model.po.resources.Product;
import nnu.mnr.satellite.mapper.resources.IProductRepo;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    @Autowired
    private ModelMapper productModelMapper;

    private final IProductRepo productRepo;

    public ProductDataService(IProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    public List<ProductInfoVO> getProductBySensorId(String sensorId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("product_id", "product_name")
                .eq("sensor_id", sensorId);
        List<Product> products = productRepo.selectList(queryWrapper);
        if (products == null) {
            return Collections.emptyList();
        }
        return productModelMapper.map(products, new TypeToken<List<ProductInfoVO>>() {}.getType());
    }

    public ProductDesVO getProductDescription(String productId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("resolution", "period", "description").eq("product_id", productId);
        Product product = productRepo.selectOne(queryWrapper);
        if (product == null) {
            return null;
        }
        return productModelMapper.map(product, new TypeToken<ProductDesVO>() {}.getType());
    }
}
