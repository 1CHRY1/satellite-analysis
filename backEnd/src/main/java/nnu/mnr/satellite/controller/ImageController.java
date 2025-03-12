package nnu.mnr.satellite.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.service.ImageDataService;
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
 * @Date: 2025/3/12 9:21
 * @Description:
 */

@RestController
@RequestMapping("api/v1/data/image")
@Slf4j
public class ImageController {

    private final ImageDataService imageDataService;

    public ImageController(ImageDataService imageDataService) {
        this.imageDataService = imageDataService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<List<Image>> GetAllData(@PathVariable String productId) {
        return ResponseEntity.ok(imageDataService.getImageByProductId(productId));
    }

}
