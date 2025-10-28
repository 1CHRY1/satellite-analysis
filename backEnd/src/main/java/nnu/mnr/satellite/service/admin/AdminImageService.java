package nnu.mnr.satellite.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.resources.IImageRepo;
import nnu.mnr.satellite.model.dto.admin.image.ImageDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.image.ImageUpdateDTO;
import nnu.mnr.satellite.model.po.resources.Image;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AdminImageService {

    @Autowired
    private IImageRepo imageRepo;

    public CommonResultVO getImagesBySceneId(String sceneId) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("image_id", "scene_id", "band", "cloud", "bucket", "tif_path").eq("scene_id", sceneId);
        List<Image> images = imageRepo.selectList(queryWrapper);
        return CommonResultVO.builder()
                .status(1)
                .message("获取波段信息成功")
                .data(images)
                .build();
    }

    public CommonResultVO updateImageInfo(ImageUpdateDTO imageUpdateDTO){
        if (imageUpdateDTO.getImageId() == null){
            return CommonResultVO.builder()
                    .status(-1)
                    .message("更新b波段信息失败，ImageId不能为空")
                    .build();
        }
        Image image = new Image();
        BeanUtils.copyProperties(imageUpdateDTO, image);
        imageRepo.updateById(image);
        return CommonResultVO.builder()
                .status(1)
                .message("更新波段信息成功")
                .build();
    }

    public CommonResultVO deleteImage(ImageDeleteDTO imageDeleteDTO){
        List<String> imageIds = imageDeleteDTO.getImageIds();
        imageRepo.deleteByIds(imageIds);
        return CommonResultVO.builder()
                .status(1)
                .message("删除波段成功")
                .build();
    }


}
