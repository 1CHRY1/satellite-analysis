package nnu.mnr.satellite.service.admin;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.resources.ICaseRepo;
import nnu.mnr.satellite.model.dto.admin.case_.CaseDeleteDTO;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;
import nnu.mnr.satellite.model.po.resources.Case;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CaseInfoVO;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AdminCaseService {

    @Autowired
    private ICaseRepo caseRepo;
    @Autowired
    private ModelMapper caseModelMapper;
    @Autowired
    private MinioUtil minioUtil;
    // 与getCasePage有关的3个函数直接拷贝CaseDataService里的
    public CommonResultVO getCasePage(CasePageDTO casePageDTO) {
        // 构造分页对象
        Page<Case> page = new Page<>(casePageDTO.getPage(), casePageDTO.getPageSize());
        // 调用 Mapper 方法
        IPage<Case> casePage = getCasesWithCondition(page, casePageDTO);
        return CommonResultVO.builder()
                .status(1)
                .message("分页查询成功")
                .data(mapPage(casePage))
                .build();
    }

    private IPage<Case> getCasesWithCondition(Page<Case> page, CasePageDTO casePageDTO) {
        Integer resolution = casePageDTO.getResolution();
        LocalDateTime startTime = casePageDTO.getStartTime();
        LocalDateTime endTime = casePageDTO.getEndTime();
        String searchText = casePageDTO.getSearchText();
        String sortField = casePageDTO.getSortField();
        Boolean asc = casePageDTO.getAsc();
        Integer regionId = casePageDTO.getRegionId();

        LambdaQueryWrapper<Case> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 按regionId筛选子区域
        if (regionId != null) {
            if (regionId % 10000 == 0) {
                lambdaQueryWrapper.between(Case::getRegionId, regionId, regionId + 9999);
            } else if (regionId % 100 == 0) {
                lambdaQueryWrapper.between(Case::getRegionId, regionId, regionId + 99);
            } else {
                lambdaQueryWrapper.eq(Case::getRegionId, regionId);
            }
        }
        // 添加时间范围筛选条件
        if (startTime != null && endTime != null) {
            lambdaQueryWrapper.between(Case::getCreateTime, startTime, endTime);
        }
        // 添加分辨率过滤条件
        if (resolution != null) {
            lambdaQueryWrapper.eq(Case::getResolution, resolution);
        }
        // 添加状态过滤条件
        lambdaQueryWrapper.eq(casePageDTO.getStatus() != null, Case::getStatus, casePageDTO.getStatus());
        // 添加搜索条件
        if (searchText != null && !searchText.isEmpty()) {
            lambdaQueryWrapper.and(wrapper -> wrapper
                    .like(Case::getAddress, searchText)  // 自动映射为数据库字段 address
                    .or()
                    .like(Case::getResolution, searchText) // 自动映射为 resolution
            );
        }
        // 添加排序条件
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "createTime":
                    lambdaQueryWrapper.orderBy(true, asc, Case::getCreateTime);
                    break;
                case "regionId":
                    lambdaQueryWrapper.orderBy(true, asc, Case::getRegionId);
                    break;
                case "resolution":
                    lambdaQueryWrapper.orderBy(true, asc, Case::getResolution);
                    break;
                // 可以根据需要添加更多的字段
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }

        return caseRepo.selectPage(page, lambdaQueryWrapper);
    }

    private IPage<CaseInfoVO> mapPage(IPage<Case> casePage) {
        // 映射记录列表
        Type destinationType = new TypeToken<List<CaseInfoVO>>() {
        }.getType();
        List<CaseInfoVO> caseInfoVOList = caseModelMapper.map(casePage.getRecords(), destinationType);

        // 创建一个新的 Page 对象
        Page<CaseInfoVO> resultPage = new Page<>();
        resultPage.setRecords(caseInfoVOList);
        resultPage.setTotal(casePage.getTotal());
        resultPage.setSize(casePage.getSize());
        resultPage.setCurrent(casePage.getCurrent());

        return resultPage;
    }

    public CommonResultVO deleteCase(CaseDeleteDTO caseDeleteDTO){
        List<String> caseIds = caseDeleteDTO.getCaseIds();
        caseIds.forEach(caseId -> {
            Case caseObj = caseRepo.selectById(caseId);
            if (caseObj != null) {
                JSONObject result = caseObj.getResult();
                if (result != null && !result.isEmpty()) {
                    String bucket = result.get("bucket").toString();
                    String objectPath = result.get("Object_path").toString();
                    minioUtil.delete(objectPath, bucket);
                }
                caseRepo.deleteById(caseId);
            }
        });
        return CommonResultVO.builder()
                .status(1)
                .message("记录删除成功")
                .build();
    }
}
