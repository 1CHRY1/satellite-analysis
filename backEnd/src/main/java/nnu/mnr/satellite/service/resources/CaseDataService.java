package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import nnu.mnr.satellite.mapper.resources.ICaseRepo;
import nnu.mnr.satellite.model.po.resources.Case;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CaseInfoVO;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
//分页
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.modelmapper.ModelMapper;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @name: CaseDataService
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/4/2025 3:34 PM
 * @version: 1.0
 */
@Service("CaseDataService")
public class CaseDataService {

    private final ICaseRepo caseRepo;
    private final ModelMapper caseModelMapper;
    private final RegionDataService regionDataService;


    public CaseDataService(ICaseRepo caseRepo, ModelMapper caseModelMapper, RegionDataService regionDataService) {
        this.caseRepo = caseRepo;
        this.caseModelMapper = caseModelMapper;
        this.regionDataService = regionDataService;
    }

    public void addCaseFromParamAndCaseId(String caseId, JSONObject param) {
        Integer resolution = (Integer) param.get("resolution");
        String address = param.get("address").toString();
        List<String> sceneIds = (List<String>) param.get("sceneIds");

        Case caseObj = Case.builder()
                .caseId(caseId)
                .address(address)
                .regionId((Integer) param.get("regionId"))
                .resolution(resolution.toString())
                .boundary((Geometry) param.get("boundary"))
                .sceneList(sceneIds)
                .dataSet(param.get("dataSet").toString())
                .status("RUNNING")
                .result(null)
                .createTime(LocalDateTime.now())
                .build();

        caseRepo.insertCase(caseObj);
    }

    public void updateCaseStatusById(String caseId, String status) {
        Case caseObj = caseRepo.selectById(caseId);
        if (caseObj == null) {
            return;
        }
        caseObj.setStatus(status);
        caseRepo.updateCaseById(caseObj);
    }

    public void updateCaseResultById(String caseId, JSONObject result) {
        Case caseObj = caseRepo.selectById(caseId);
        if (caseObj == null) {
            return;
        }
        caseObj.setResult(result);
        caseRepo.updateCaseById(caseObj);
    }

    public void removeCaseById(String caseId) {
        caseRepo.deleteById(caseId);
    }

    public Case selectById(String caseId) {
        return caseRepo.selectById(caseId);
    }

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

        // TODO: DEPRECATED 查询 region_table 表，获取当前行政区下所有regionId
        /*List<Integer> allRegionIds = new ArrayList<>();
        if (regionId != null) {
            allRegionIds = regionDataService.getAllRegionIdsByParent(regionId, allRegionIds);
        } else {
            allRegionIds = null;
        }
        // 添加区域条件
        if (allRegionIds != null && !allRegionIds.isEmpty()) {
            List<Integer> finalCollection = allRegionIds;
            lambdaQueryWrapper.in(Case::getRegionId, finalCollection);
        }*/

        // 模糊查询address
        // lambdaQueryWrapper.like(Case::getAddress, casePageDTO.getAddress());

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

    private List<Integer> getAcroutesByRegionId(Integer regionId) {
        Region region = regionDataService.getRegionById(regionId);

        if (region != null && region.getAcroutes() != null) {
            return region.getAcroutes();
        } else {
            return List.of(); // 返回空列表，避免 null
        }
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

    public CommonResultVO getCaseBoundaryByCaseId(String caseId) {
        Case caseEntity = caseRepo.selectById(caseId);
        // 检查是否找到 Case
        if (caseEntity == null) {
            return CommonResultVO.builder()
                    .status(0)
                    .message("未找到对应的 Case")
                    .data(null)
                    .build();
        }
        // 提取 boundary 字段
        Geometry boundary = caseEntity.getBoundary();
        JSONObject JSONBoundary;

        try {
            JSONBoundary = GeometryUtil.geometry2Geojson(boundary);
        } catch (IOException e) {
            // 处理异常，例如记录日志或返回错误信息
            System.err.println("转换 Geometry 到 GeoJSON 时发生错误: " + e.getMessage());
            return CommonResultVO.builder()
                    .status(0)
                    .message("转换 Geometry 到 GeoJSON 时发生错误")
                    .data(null)
                    .build();
        }

        // 构建返回结果
        return CommonResultVO.builder()
                .status(1)
                .message("boundary查询成功")
                .data(JSONBoundary)
                .build();
    }

    public CommonResultVO getCaseByCaseId(String caseId) {
        Case caseEntity = caseRepo.selectById(caseId);
        // 检查是否找到 Case
        if (caseEntity == null) {
            return CommonResultVO.builder()
                    .status(1)
                    .message("未找到对应的 Case")
                    .data(null)
                    .build();
        } else {
            return CommonResultVO.builder()
                    .status(1)
                    .message("success")
                    .data(caseModelMapper.map(caseEntity, CaseInfoVO.class))
                    .build();
        }
    }

    public CommonResultVO getResultByCaseId(String caseId) {
        Case caseEntity = caseRepo.selectById(caseId);
        // 检查是否找到 Case
        if (caseEntity == null) {
            return CommonResultVO.builder()
                    .status(1)
                    .message("未找到对应的 Case")
                    .data(null)
                    .build();
        } else {
            return CommonResultVO.builder()
                    .status(1)
                    .message("success")
                    .data(caseEntity.getResult())
                    .build();
        }
    }

}
