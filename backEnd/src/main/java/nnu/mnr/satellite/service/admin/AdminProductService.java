package nnu.mnr.satellite.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.resources.IProductRepo;
import nnu.mnr.satellite.mapper.resources.ISensorRepo;
import nnu.mnr.satellite.model.dto.admin.product.ProductDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.product.ProductInsertDTO;
import nnu.mnr.satellite.model.dto.admin.product.ProductPageDTO;
import nnu.mnr.satellite.model.dto.admin.product.ProductUpdateDTO;
import nnu.mnr.satellite.model.po.resources.Product;
import nnu.mnr.satellite.model.vo.admin.ProductInfoWithSensorVO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AdminProductService {

    @Autowired
    private IProductRepo productRepo;

    @Autowired
    private ISensorRepo sensorRepo;

    public CommonResultVO getProductInfoPage(ProductPageDTO productPageDTO) {
        // 构造分页对象
        Page<Product> page = new Page<>(productPageDTO.getPage(), productPageDTO.getPageSize());
        LambdaQueryWrapper<Product> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 筛选
        List<String> sensorIds = productPageDTO.getSensorIds();
        if (sensorIds != null && !sensorIds.isEmpty()) {
            String inClause = String.join(",",
                    sensorIds.stream().map(id -> "'" + id.replace("'", "''") + "'").toList()
            );
            lambdaQueryWrapper.apply("p.sensor_id IN (" + inClause + ")");
        }
        String searchText = productPageDTO.getSearchText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String trimmedSearchText = searchText.trim();
            lambdaQueryWrapper.and(wrapper ->
                    wrapper.like(Product::getProductName, trimmedSearchText)
            );
        }

        // 排序
        String sortField = productPageDTO.getSortField();
        Boolean asc = productPageDTO.getAsc();
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "productId":
                    lambdaQueryWrapper.orderBy(true, asc, Product::getProductId);
                    break;
                case "productName":
                    lambdaQueryWrapper.orderBy(true, asc, Product::getProductName);
                    break;
                case "sensorId":
                    lambdaQueryWrapper.orderBy(true, asc, Product::getSensorId);
                    break;
                case "resolution":
                    lambdaQueryWrapper.last(
                            "ORDER BY CAST(split_part(p.resolution, 'm', 1) AS FLOAT) "
                                    + (asc ? "ASC NULLS LAST" : "DESC NULLS LAST")
                    );
                    break;
                case "period":
                    lambdaQueryWrapper.last(
                            "ORDER BY CAST(split_part(p.period, 'd', 1) AS INTEGER) "
                                    + (asc ? "ASC NULLS LAST" : "DESC NULLS LAST")
                    );
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        // 查询，lambdaQueryWrapper没有显式指定selecet，默认select *
        IPage<ProductInfoWithSensorVO> productPage = productRepo.selectProductPageWithSensorName(page, lambdaQueryWrapper);

        return CommonResultVO.builder()
                .status(1)
                .message("产品信息获取成功")
                .data(productPage)
                .build();
    }

    public CommonResultVO insertProduct(ProductInsertDTO productInsertDTO){
        if (!sensorRepo.existsBySensorId(productInsertDTO.getSensorId())){
            return CommonResultVO.builder()
                    .status(-1)
                    .message("对应传感器不存在，新增产品失败")
                    .build();
        }
        Product product = new Product();
        BeanUtils.copyProperties(productInsertDTO, product);
        String productId = IdUtil.generateProductId();
        product.setProductId(productId);
        productRepo.insert(product);
        return CommonResultVO.builder()
                .status(1)
                .message("新增产品成功")
                .build();
    }

    public CommonResultVO updateProduct(ProductUpdateDTO productUpdateDTO){
        if (!sensorRepo.existsBySensorId(productUpdateDTO.getSensorId())){
            return CommonResultVO.builder()
                    .status(-1)
                    .message("对应传感器不存在，更改产品失败")
                    .build();
        }
        Product product = new Product();
        BeanUtils.copyProperties(productUpdateDTO, product);
        productRepo.updateById(product);
        return CommonResultVO.builder()
                .status(1)
                .message("更新产品信息成功")
                .build();
    }

    public CommonResultVO deleteProduct(ProductDeleteDTO productDeleteDTO){
        List<String> productIds = productDeleteDTO.getProductIds();
        productRepo.deleteByIds(productIds);
        return CommonResultVO.builder()
                .status(1)
                .message("删除产品成功")
                .build();
    }
}
