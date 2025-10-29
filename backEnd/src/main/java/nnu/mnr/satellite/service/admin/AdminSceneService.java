package nnu.mnr.satellite.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.resources.IImageRepo;
import nnu.mnr.satellite.mapper.resources.ISceneRepo;
import nnu.mnr.satellite.model.dto.admin.image.ImagePathsDTO;
import nnu.mnr.satellite.model.dto.admin.scene.CloudPathsDTO;
import nnu.mnr.satellite.model.dto.admin.scene.SceneDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.scene.ScenePageDTO;
import nnu.mnr.satellite.model.dto.admin.scene.SceneUpdateDTO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.vo.admin.SceneInfoVO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AdminSceneService {

    @Autowired
    private ISceneRepo sceneRepo;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private IImageRepo imageRepo;

    public CommonResultVO getSceneInfoPage(ScenePageDTO scenePageDTO){
        // 构造分页对象
        Page<Scene> page = new Page<>(scenePageDTO.getPage(), scenePageDTO.getPageSize());

        String sortField = scenePageDTO.getSortField();
        Boolean asc = scenePageDTO.getAsc();
        List<String> sensorIds = scenePageDTO.getSensorIds();
        String productId = scenePageDTO.getProductId();
        LocalDateTime startTime = scenePageDTO.getStartTime();
        LocalDateTime endTime = scenePageDTO.getEndTime();
        String searchText = scenePageDTO.getSearchText();
        LambdaQueryWrapper<Scene> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 1. 添加传感器ID筛选条件（支持多个sensorId）
        if (sensorIds != null && !sensorIds.isEmpty()) {
            lambdaQueryWrapper.in(Scene::getSensorId, sensorIds);
        }

        // 2. 添加产品ID筛选条件
        if (productId != null && !productId.trim().isEmpty()) {
            lambdaQueryWrapper.eq(Scene::getProductId, productId.trim());
        }

        // 3. 添加时间范围筛选条件
        if (startTime != null) {
            lambdaQueryWrapper.ge(Scene::getSceneTime, startTime);
        }
        if (endTime != null) {
            lambdaQueryWrapper.le(Scene::getSceneTime, endTime);
        }

        if (searchText != null && !searchText.trim().isEmpty()) {
            String trimmedSearchText = searchText.trim();
            lambdaQueryWrapper.and(wrapper ->
                    wrapper.like(Scene::getSceneName, trimmedSearchText)
            );
        }

        // 排序
        if (sortField != null && !sortField.isEmpty()) {
            switch (sortField) {
                case "sceneName":
                    lambdaQueryWrapper.orderBy(true, asc, Scene::getSceneName);
                    break;
                case "sceneTime":
                    lambdaQueryWrapper.orderBy(true, asc, Scene::getSceneTime);
                    break;
                case "sensorId":
                    lambdaQueryWrapper.orderBy(true, asc, Scene::getSensorId);
                    break;
                case "bandNum":
                    lambdaQueryWrapper.orderBy(true, asc, Scene::getBandNum);
                    break;
                case "cloud":
                    lambdaQueryWrapper.orderBy(true, asc, Scene::getCloud);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        IPage<Scene> scenePage = sceneRepo.selectPage(page, lambdaQueryWrapper);
        IPage<SceneInfoVO> sceneInfoVOPage = scenePage.convert(scene -> {
            SceneInfoVO sceneInfoVO = new SceneInfoVO();
            BeanUtils.copyProperties(scene, sceneInfoVO);
            return sceneInfoVO;
        });

        return CommonResultVO.builder()
                .status(1)
                .message("景信息获取成功")
                .data(sceneInfoVOPage)
                .build();
    }

    public CommonResultVO updateSceneInfo(SceneUpdateDTO sceneUpdateDTO){
        if (sceneUpdateDTO.getSceneId() == null){
            return CommonResultVO.builder()
                    .status(-1)
                    .message("更新景信息失败，sceneId不能为空")
                    .build();
        }
        Scene scene = new Scene();
        BeanUtils.copyProperties(sceneUpdateDTO, scene);
        sceneRepo.updateById(scene);
        return CommonResultVO.builder()
                .status(1)
                .message("更新景信息成功")
                .build();
    }

    public CommonResultVO deleteScene(SceneDeleteDTO sceneDeleteDTO) {
        List<String> sceneIds = sceneDeleteDTO.getSceneIds();

        // 1. 查询场景相关的文件路径信息
        List<CloudPathsDTO> filePaths = sceneRepo.findFilePathsBySceneIds(sceneIds);
        List<ImagePathsDTO> imagePaths = imageRepo.findImagePathsBySceneId(sceneIds);

        // 2. 删除MinIO中的文件
        for (CloudPathsDTO paths : filePaths) {
            // 删除场景文件（如果有）
            String bucket = paths.getBucket();
            String cloudPath = paths.getCloudPath();
            if (bucket != null && cloudPath != null) {
                minioUtil.delete(cloudPath, bucket);
            }
        }
        for (ImagePathsDTO imagePath : imagePaths) {
            String bucket = imagePath.getBucket();
            String tifPath = imagePath.getTifPath();
            if (imagePath.getBucket() != null && imagePath.getTifPath() != null) {
                minioUtil.delete(tifPath, bucket);
            }
        }

        // 3. 删除数据库记录
        sceneRepo.deleteByIds(sceneIds);

        return CommonResultVO.builder()
                .status(1)
                .message("删除景和对应波段成功")
                .build();
    }
}
