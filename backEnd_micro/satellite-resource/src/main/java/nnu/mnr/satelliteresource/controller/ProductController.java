package nnu.mnr.satelliteresource.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satelliteresource.model.vo.resources.ProductDesVO;
import nnu.mnr.satelliteresource.model.vo.resources.ProductInfoVO;
import nnu.mnr.satelliteresource.service.ProductDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:10
 * @Description:
 */

@RestController
@RequestMapping("api/v1/data/product")
@Slf4j
public class ProductController {

    private final ProductDataService productDataService;

    public ProductController(ProductDataService productDataService) {
        this.productDataService = productDataService;
    }

    @GetMapping("/sensorId/{sensorId}")
    public ResponseEntity<List<ProductInfoVO>> getProductBySensorId(@PathVariable String sensorId) {
        return ResponseEntity.ok(productDataService.getProductBySensorId(sensorId));
    }

    @GetMapping("/description/productId/{productId}")
    public ResponseEntity<ProductDesVO> GetSensorDescription(@PathVariable String productId) {
        return ResponseEntity.ok(productDataService.getProductDescription(productId));
    }

}
