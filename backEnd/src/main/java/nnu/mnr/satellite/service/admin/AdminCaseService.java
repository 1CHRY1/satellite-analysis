package nnu.mnr.satellite.service.admin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.modeling.IMethlibCaseRepo;
import nnu.mnr.satellite.mapper.resources.ICaseRepo;
import nnu.mnr.satellite.model.dto.admin.case_.CaseDeleteDTO;
import nnu.mnr.satellite.model.dto.modeling.MethlibCasePageDTO;
import nnu.mnr.satellite.model.dto.resources.CasePageDTO;
import nnu.mnr.satellite.model.po.modeling.MethlibCase;
import nnu.mnr.satellite.model.po.resources.Case;
import nnu.mnr.satellite.model.pojo.modeling.MinioProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CaseInfoVO;
import nnu.mnr.satellite.service.modeling.MethlibService;
import nnu.mnr.satellite.service.resources.CaseDataService;
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
    private CaseDataService caseDataService;
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private MethlibService methlibService;
    @Autowired
    private IMethlibCaseRepo methlibCaseRepo;
    @Autowired
    private MinioProperties minioProperties;

    public CommonResultVO getCasePage(CasePageDTO casePageDTO, String userId) {
       return caseDataService.getCasePage(casePageDTO, userId);
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

    public CommonResultVO getMethlibCasePage(MethlibCasePageDTO methlibCasePageDTO, String userId) {
        return methlibService.getMethlibCasePage(methlibCasePageDTO, userId);
    }

    public CommonResultVO deleteMethlibCase(CaseDeleteDTO caseDeleteDTO){
        String prefix = minioProperties.getUrl();
        List<String> caseIds = caseDeleteDTO.getCaseIds();
        caseIds.forEach(caseId -> {
            MethlibCase caseObj = methlibCaseRepo.selectById(caseId);
            if (caseObj != null) {
                JSONObject result = caseObj.getResult();
                if (result != null && !result.isEmpty()) {
                    for (String key : result.keySet()) {
                        JSONArray urlArray = result.getJSONArray(key);
                        if (urlArray != null) {
                            for (int i = 0; i < urlArray.size(); i++) {
                                String url = urlArray.getString(i);
                                if (url != null && !url.isEmpty() && url.startsWith(prefix)) {
                                    // 加1是因为顺便去除斜杠
                                    String pathAfterPrefix = url.substring(prefix.length()+1);
                                    String bucket = pathAfterPrefix.split("/")[0];
                                    String fileName = pathAfterPrefix.substring(bucket.length()+1);
                                    minioUtil.delete(fileName, bucket);
                                }
                            }
                        }
                    }
                }
                methlibCaseRepo.deleteById(caseId);
            }
        });
        return CommonResultVO.builder()
                .status(1)
                .message("记录删除成功")
                .build();
    }
}
