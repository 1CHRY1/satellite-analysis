package nnu.mnr.satellite.controller;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.model.po.Image;
import nnu.mnr.satellite.service.resources.ImageDataService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @GetMapping("/{sceneId}")
    public ResponseEntity<List<Image>> getImageBySceneId(@PathVariable String sceneId) {
        return ResponseEntity.ok(imageDataService.getImagesBySceneId(sceneId));
    }

    @GetMapping("/{imageId}/tif")
    public ResponseEntity<byte[]> getTifByImageId(@PathVariable String imageId) {
        byte[] tifData = imageDataService.getTifByImageId(imageId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/tiff"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(tifData);
    }

}
