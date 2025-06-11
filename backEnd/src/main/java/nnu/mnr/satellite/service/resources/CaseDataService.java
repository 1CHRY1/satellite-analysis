package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import nnu.mnr.satellite.mapper.resources.ICaseRepo;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.po.resources.Case;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//分页
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;

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
    private final GeometryTypeHandler geometryTypeHandler;

    public CaseDataService(ICaseRepo caseRepo, GeometryTypeHandler geometryTypeHandler) {
        this.caseRepo = caseRepo;
        this.geometryTypeHandler = geometryTypeHandler;
    }

    public void addCaseFromParamAndCaseId(String caseId, JSONObject param) {
        Integer resolution = (Integer) param.get("resolution");
        String caseName = param.get("address").toString() + resolution + "米无云一版图";
        List<String> sceneIds = (List<String>) param.get("sceneIds");

        Case caseObj = Case.builder()
                .caseId(caseId)
                .caseName(caseName)
                .resolution(resolution.toString())
                .boundary((Geometry) param.get("boundary"))
                .sceneList(sceneIds)
                .status("RUNNING")
                .result(null)
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

    public IPage<Case> getCasePage(PageDTO pageDTO) {
        // 构造分页对象
        Page<Case> page = new Page<>(pageDTO.getPage(), pageDTO.getPageSize());

        // 调用 Mapper 方法
        IPage<Case> casePage = caseRepo.selectPageWithCondition(
                page,
                pageDTO.getSearchText(),
                pageDTO.getSortField(),
                pageDTO.getAsc()
        );
        if (casePage != null && casePage.getRecords() != null) {
            for (Case caseItem : casePage.getRecords()) {
                if (caseItem != null && caseItem.getBoundary() != null) {
                    String wkt = geometryTypeHandler.geometryToWKT(caseItem.getBoundary());
                    caseItem.setBoundaryText(wkt);
                    caseItem.setBoundary(null);
                }
            }
        }
        return casePage;
    }
}
