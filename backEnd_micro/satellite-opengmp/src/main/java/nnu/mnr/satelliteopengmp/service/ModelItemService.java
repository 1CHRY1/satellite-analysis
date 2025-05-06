package nnu.mnr.satelliteopengmp.service;

import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satelliteopengmp.model.dto.ResourcePageDTO;
import nnu.mnr.satelliteopengmp.model.po.ModelItem;
import nnu.mnr.satelliteopengmp.model.vo.CommonResultVO;
import nnu.mnr.satelliteopengmp.model.vo.ModelItemVO;
import nnu.mnr.satelliteopengmp.repository.IModelItemRepo;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/31 10:13
 * @Description:
 */

@Service
@Slf4j
public class ModelItemService {

    private final IModelItemRepo modelItemRepo;

    @Autowired
    ModelMapper modelItemMapper;

    @Autowired
    CommonService commonService;

    public ModelItemService(IModelItemRepo modelItemRepo) {
        this.modelItemRepo = modelItemRepo;
    }

    public ModelItemVO getModelItemById(String mid) {
        Optional<ModelItem> modelItem = modelItemRepo.findById(mid);
        return modelItem.<ModelItemVO>map(
                item -> modelItemMapper.map(
                        item, new TypeToken<ModelItemVO>() {}.getType()))
                .orElse(null);
    }

    public CommonResultVO getResourceMethodList(ResourcePageDTO resourcePageDTO) {
        try{
            Pageable pageable = commonService.getResourcePageable(resourcePageDTO);
            Page<ModelItem> modelItemPage = modelItemRepo.findAllByModelTypeAndNameLikeIgnoreCaseAndNormalTagsContainingIgnoreCase("method",resourcePageDTO.getSearchText(), resourcePageDTO.getTagNames(),pageable);
            Page<ModelItemVO> modelItemVOPage = modelItemPage.map(
                    modelItem -> modelItemMapper.map(modelItem, ModelItemVO.class)
            );
            return CommonResultVO.builder().status(1).message("获取数据方法成功").data(modelItemVOPage.getContent()).build();
        }catch (Exception e){
            log.error(e.getMessage());
            return CommonResultVO.builder().status(-1).message("获取数据方法失败").build();
        }
    }

    public CommonResultVO getResourceModelList(ResourcePageDTO resourcePageDTO) {
        try{
            Pageable pageable = commonService.getResourcePageable(resourcePageDTO);
            String tagClass= resourcePageDTO.getTagClass();
            Page<ModelItem> modelItemPage = modelItemRepo.findAllByModelTypeAndNameLikeIgnoreCaseAndNormalTagsContainingIgnoreCase("model",resourcePageDTO.getSearchText(), resourcePageDTO.getTagNames(), pageable);
            Page<ModelItemVO> modelItemVOPage = modelItemPage.map(
                    modelItem -> modelItemMapper.map(modelItem, ModelItemVO.class)
            );
            return CommonResultVO.builder().status(1).message("获取模型数据成功").data(modelItemVOPage.getContent()).build();
        }catch (Exception e){
            log.error(e.getMessage());
            return CommonResultVO.builder().status(-1).message("保存模型数据失败").build();
        }
    }

}
