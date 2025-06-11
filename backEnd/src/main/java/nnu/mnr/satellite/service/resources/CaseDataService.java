package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.mapper.resources.ICaseRepo;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.po.resources.Case;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CaseInfoVO;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
//分页
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;
import org.modelmapper.ModelMapper;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
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


    public CaseDataService(ICaseRepo caseRepo, ModelMapper caseModelMapper) {
        this.caseRepo = caseRepo;
        this.caseModelMapper = caseModelMapper;
    }

    public void addCaseFromParamAndCaseId(String caseId, JSONObject param) {
        Integer resolution = (Integer) param.get("resolution");
        String caseName = param.get("address").toString() + resolution + "km格网无云一版图";
        List<String> sceneIds = (List<String>) param.get("sceneIds");

        Case caseObj = Case.builder()
                .caseId(caseId)
                .caseName(caseName)
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
        caseObj.setStatus(status);
        caseRepo.updateCaseById(caseObj);
    }

    public void updateCaseResultById(String caseId, JSONObject result) {
        Case caseObj = caseRepo.selectById(caseId);
        caseObj.setResult(result);
        caseRepo.updateCaseById(caseObj);
    }

    public void removeCaseById(String caseId) {
        caseRepo.deleteById(caseId);
    }

    public Case selectById(String caseId) {
        return caseRepo.selectById(caseId);
    }

    public CommonResultVO getCasePage(PageDTO pageDTO) {
        // 构造分页对象
        Page<Case> page = new Page<>(pageDTO.getPage(), pageDTO.getPageSize());
        QueryWrapper<Case> queryWrapper = new QueryWrapper<>();
        // 调用 Mapper 方法
        IPage<Case> casePage = getCasesWithCondition(
                page,
                pageDTO.getSearchText(),
                pageDTO.getSortField(),
                pageDTO.getAsc()
        );
        return CommonResultVO.builder()
                .status(1)
                .message("分页查询成功")
                .data(mapPage(casePage))
                .build();
    }
    private IPage<Case> getCasesWithCondition(Page<Case> page, String searchText, String sortField, Boolean asc) {
        LambdaQueryWrapper<Case> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 添加搜索条件（使用Lambda表达式引用实体类属性）
        if (searchText != null && !searchText.isEmpty()) {
            lambdaQueryWrapper.and(wrapper -> wrapper
                    .like(Case::getCaseName, searchText)  // 自动映射为数据库字段 case_name
                    .or()
                    .like(Case::getResolution, searchText) // 自动映射为 resolution
            );
        }

        // 添加排序条件
        if (sortField != null && !sortField.isEmpty()) {
            if ("createTime".equals(sortField)) {
                lambdaQueryWrapper.orderBy(true, asc, Case::getCreateTime);
            } else {
                // 其他字段可以通过反射或switch处理，这里简化处理
                throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }

        return caseRepo.selectPage(page, lambdaQueryWrapper);
    }
    private IPage<CaseInfoVO> mapPage(IPage<Case> casePage) {
        // 映射记录列表
        Type destinationType = new TypeToken<List<CaseInfoVO>>() {}.getType();
        List<CaseInfoVO> caseInfoVOList = caseModelMapper.map(casePage.getRecords(), destinationType);

        // 创建一个新的 Page 对象
        Page<CaseInfoVO> resultPage = new Page<>();
        resultPage.setRecords(caseInfoVOList);
        resultPage.setTotal(casePage.getTotal());
        resultPage.setSize(casePage.getSize());
        resultPage.setCurrent(casePage.getCurrent());

        return resultPage;
    }

}
