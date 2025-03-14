package nnu.mnr.satellite.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.Product;
import nnu.mnr.satellite.service.resources.ProductDataService;
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
    public ResponseEntity<List<Product>> getProductBySensorId(@PathVariable String sensorId) {
        return ResponseEntity.ok(productDataService.getProductBySensorId(sensorId));
    }

    @GetMapping("/description/productId/{productId}")
    public ResponseEntity<List<Product>> GetSensorDescription(@PathVariable String productId) {
        return ResponseEntity.ok(productDataService.getProductDescription(productId));
    }

}
