package nnu.mnr.satelliteresource.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satelliteresource.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satelliteresource.model.po.Image;
import nnu.mnr.satelliteresource.model.vo.resources.ImageInfoVO;
import nnu.mnr.satelliteresource.repository.IImageRepo;
import nnu.mnr.satelliteresource.utils.data.MinioUtil;
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

    public byte[] getTifByImageId(String imageId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("image_id", imageId);
        Image image = imageRepo.selectOne(queryWrapper);
        return minioUtil.downloadByte(image.getBucket(), image.getTifPath());
    }

    public List<ModelServerImageDTO> getModelServerImageDTOBySceneId(String sceneId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scene_id", sceneId);
        List<Image> images = imageRepo.selectList(queryWrapper);
//        return imageModelMapper.map(images, new TypeToken<List<ModelServerImageDTO>>() {}.getType());
        return images.stream()
                .map(image -> ModelServerImageDTO.builder()
                        .tifPath(image.getTifPath())
                        .band(image.getBand())
                        .build())
                .collect(Collectors.toList());
    }

}
