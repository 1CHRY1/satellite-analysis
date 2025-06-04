package nnu.mnr.satellite.service.resources;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.vo.resources.ImageInfoVO;
import nnu.mnr.satellite.model.po.resources.Image;
import nnu.mnr.satellite.mapper.resources.IImageRepo;
import nnu.mnr.satellite.utils.data.MinioUtil;
import org.json.simple.JSONObject;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 9:20
 * @Description:
 */

@Service("ImageDataService")
public class ImageDataService {

    @Autowired
    private ModelMapper imageModelMapper;

    @Autowired
    MinioUtil minioUtil;

    private final IImageRepo imageRepo;

    public ImageDataService(IImageRepo imageRepo) {
        this.imageRepo = imageRepo;
    }

    public List<ImageInfoVO> getImagesBySceneId(String sceneId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("image_id", "band", "cloud").eq("scene_id", sceneId);
        List<Image> images = imageRepo.selectList(queryWrapper);
        return imageModelMapper.map(images, new TypeToken<List<ImageInfoVO>>() {}.getType());
    }

    public List<ModelServerImageDTO> getModelServerImageDTOBySceneId(String sceneId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("tif_path", "band", "bucket").eq("scene_id", sceneId);
        List<Image> images = imageRepo.selectList(queryWrapper);
//        return imageModelMapper.map(images, new TypeToken<List<ModelServerImageDTO>>() {}.getType());
        return images.stream()
                .map(image -> ModelServerImageDTO.builder()
                        .tifPath(image.getTifPath())
                        .band(image.getBand())
                        .bucket(image.getBucket())
                        .build())
                .collect(Collectors.toList());

    }

    public byte[] getTifByImageId(String imageId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("image_id", imageId);
        Image image = imageRepo.selectOne(queryWrapper);
        return minioUtil.downloadByte(image.getBucket(), image.getTifPath());
    }

}
