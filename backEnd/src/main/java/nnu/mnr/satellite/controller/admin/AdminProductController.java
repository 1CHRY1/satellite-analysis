package nnu.mnr.satellite.controller.admin;

import nnu.mnr.satellite.model.dto.admin.product.ProductDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.product.ProductInsertDTO;
import nnu.mnr.satellite.model.dto.admin.product.ProductPageDTO;
import nnu.mnr.satellite.model.dto.admin.product.ProductUpdateDTO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.service.admin.AdminProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/api/v1/product")
public class AdminProductController {

    @Autowired
    private AdminProductService adminProductService;

    @PostMapping("/page")
    public ResponseEntity<CommonResultVO> getProductInfoPage(@RequestBody ProductPageDTO productPageDTO) throws Exception {
        return ResponseEntity.ok(adminProductService.getProductInfoPage(productPageDTO));
    }

    @PutMapping("/insert")
    public ResponseEntity<CommonResultVO> insertProduct(@RequestBody ProductInsertDTO productInsertDTO) throws Exception {
        return ResponseEntity.ok(adminProductService.insertProduct(productInsertDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResultVO> updateProduct(@RequestBody ProductUpdateDTO productUpdateDTO) throws Exception {
        return ResponseEntity.ok(adminProductService.updateProduct(productUpdateDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResultVO> deleteProduct(@RequestBody ProductDeleteDTO productDeleteDTO) throws Exception {
        return ResponseEntity.ok(adminProductService.deleteProduct(productDeleteDTO));
    }

}
