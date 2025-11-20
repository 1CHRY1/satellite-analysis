package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import nnu.mnr.satellite.jobs.QuartzSchedulerManager;
import nnu.mnr.satellite.mapper.modeling.IMethlibCaseRepo;
import nnu.mnr.satellite.mapper.modeling.IMethlibRepo;
import nnu.mnr.satellite.mapper.modeling.ITagRepo;
import nnu.mnr.satellite.model.dto.modeling.MethlibCasePageDTO;
import nnu.mnr.satellite.model.dto.modeling.MethlibPageDTO;
import nnu.mnr.satellite.model.po.modeling.Methlib;
import nnu.mnr.satellite.model.po.modeling.MethlibCase;
import nnu.mnr.satellite.model.po.modeling.Tag;
import nnu.mnr.satellite.model.po.resources.Case;
import nnu.mnr.satellite.model.po.resources.Product;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.pojo.modeling.ModelServerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.modeling.MethlibInfoVO;
import nnu.mnr.satellite.model.vo.resources.CaseInfoVO;
import nnu.mnr.satellite.utils.common.ProcessUtil;
import nnu.mnr.satellite.utils.dt.RedisUtil;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class MethlibService {

    @Autowired
    private IMethlibRepo methlibRepo;
    @Autowired
    private ITagRepo tagRepo;
    @Autowired
    ModelServerProperties modelServerProperties;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    QuartzSchedulerManager quartzSchedulerManager;
    @Autowired
    IMethlibCaseRepo methlibCaseRepo;
    @Autowired
    ModelMapper methlibModelMapper;

    public CommonResultVO getMethlibPage(MethlibPageDTO methlibPageDTO){
        Integer current = methlibPageDTO.getPage();
        Integer pageSize = methlibPageDTO.getPageSize();
        List<Integer> tags = methlibPageDTO.getTags();
        String sortField = methlibPageDTO.getSortField();
        Boolean asc = methlibPageDTO.getAsc();
        String searchText = methlibPageDTO.getSearchText();
        Page<MethlibInfoVO> page = new Page<>(current, pageSize);
        IPage<MethlibInfoVO> methlibInfoVO = methlibRepo.getMethlibInfo(page, tags, sortField, asc, searchText);
        return CommonResultVO.builder()
                .status(1)
                .message("方法信息获取成功")
                .data(methlibInfoVO)
                .build();
    }

    public CommonResultVO getMethlibById(String methlibId){
        Methlib methlib = methlibRepo.selectById(methlibId);
        return CommonResultVO.builder()
                .status(1)
                .message("方法信息获取成功")
                .data(methlib)
                .build();
    }

    public CommonResultVO getAllTags() {
        List<Tag> tagList = tagRepo.selectList(null);
        return CommonResultVO.builder()
                .status(1)
                .message("标签获取成功")
                .data(tagList)
                .build();
    }

    public CommonResultVO invokeMethlib(Integer methlibId, Map<String, Object> params, String userId) {
        Methlib methlibInfo = methlibRepo.selectById(methlibId);
        JSONObject methlibParams = JSONObject.of("params", params, "method", methlibInfo, "userId", userId);
        String methlibUrl = modelServerProperties.getAddress() + modelServerProperties.getApis().get("methlib");
        long expirationTime = 60 * 100;
        return runModelServerModel(methlibUrl, methlibParams, expirationTime);
    }

    // 这里也重写一遍
    public CommonResultVO runModelServerModel(String url, JSONObject param, long expirationTime) {
        try {
            JSONObject modelCaseResponse = JSONObject.parseObject(ProcessUtil.runModelCase(url, param));
            String caseId = modelCaseResponse.getJSONObject("data").getString("taskId");
            JSONObject caseJsonObj = JSONObject.of("methodId", param.getJSONObject("method").get("id"),
                    "params", param.getJSONObject("params"),
                    "userId", param.getString("userId"));
            // 在没有相应历史记录的情况下, 持久化记录
            if (methlibCaseRepo.selectById(caseId) == null) {
                addCaseFromParamAndCaseId(caseId, caseJsonObj);
            }
            JSONObject modelCase = JSONObject.of("status", "RUNNING", "start", LocalDateTime.now());
            redisUtil.addJsonDataWithExpiration(caseId, modelCase, expirationTime);
            quartzSchedulerManager.startModelRunningStatusJob(caseId, modelServerProperties);
            return CommonResultVO.builder().status(1).message("success").data(caseId).build();
        } catch (Exception e) {
            return CommonResultVO.builder().status(-1).message("Wrong Because of " + e.getMessage()).build();
        }
    }

    public void addCaseFromParamAndCaseId(String caseId, JSONObject param) {
        Integer methodId = param.getInteger("methodId");
        JSONObject params = param.getJSONObject("params");
        String userId = param.getString("userId");

        MethlibCase methlibCaseObj = MethlibCase.builder()
                .caseId(caseId)
                .methodId(methodId)
                .params(params)
                .userId(userId)
                .status("RUNNING")
                .result(null)
                .createTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
                .build();

        methlibCaseRepo.insert(methlibCaseObj);
    }

    public void updateCaseStatusById(String caseId, String status) {
        MethlibCase methlibCaseObj = methlibCaseRepo.selectById(caseId);
        if (methlibCaseObj == null) {
            return;
        }
        methlibCaseObj.setStatus(status);
        methlibCaseRepo.updateById(methlibCaseObj);
    }

    public void updateCaseResultById(String caseId, JSONObject result) {
        MethlibCase methlibCaseObj = methlibCaseRepo.selectById(caseId);
        if (methlibCaseObj == null) {
            return;
        }
        methlibCaseObj.setResult(result);
        methlibCaseRepo.updateById(methlibCaseObj);
    }

    public CommonResultVO getMethlibCasePage(MethlibCasePageDTO methlibCasePageDTO, String userId){
        Page<MethlibCase> page = new Page<>(methlibCasePageDTO.getPage(), methlibCasePageDTO.getPageSize());
        LambdaQueryWrapper<MethlibCase> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 条件筛选
        Integer methodId = methlibCasePageDTO.getMethodId();
        String status = methlibCasePageDTO.getStatus();
        if (methodId != null) {
            lambdaQueryWrapper.eq(MethlibCase::getMethodId, methodId);
        }
        if (status != null) {
            lambdaQueryWrapper.eq(MethlibCase::getStatus, status);
        }
        if (userId != null) {
            lambdaQueryWrapper.eq(MethlibCase::getUserId, userId);
        }
        // 添加时间范围筛选条件
        LocalDateTime startTime = methlibCasePageDTO.getStartTime();
        LocalDateTime endTime = methlibCasePageDTO.getEndTime();
        if (startTime != null && endTime != null) {
            lambdaQueryWrapper.between(MethlibCase::getCreateTime, startTime, endTime);
        }
        // 添加排序条件
        String sortField = methlibCasePageDTO.getSortField();
        Boolean asc = methlibCasePageDTO.getAsc();
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "createTime":
                    lambdaQueryWrapper.orderBy(true, asc, MethlibCase::getCreateTime);
                    break;
                case "methodId":
                    lambdaQueryWrapper.orderBy(true, asc, MethlibCase::getMethodId);
                    break;
                case "status":
                    lambdaQueryWrapper.orderBy(true, asc, MethlibCase::getStatus);
                    break;
                // 可以根据需要添加更多的字段
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        IPage<MethlibCase> methlibCasePage = methlibCaseRepo.selectPage(page, lambdaQueryWrapper);
        return CommonResultVO.builder()
                .status(1)
                .message("算法历史记录获取成功")
                .data(methlibCasePage)
                .build();
    }

    public CommonResultVO getCaseByCaseId(String caseId) {
        MethlibCase caseEntity = methlibCaseRepo.selectById(caseId);
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
                    .message("历史记录获取成功")
                    .data(caseEntity)
                    .build();
        }
    }

    public CommonResultVO getResultByCaseId(String caseId) {
        MethlibCase caseEntity = methlibCaseRepo.selectById(caseId);
        // 检查是否找到 Case
        if (caseEntity == null) {
            return CommonResultVO.builder()
                    .status(1)
                    .message("未找到对应的 Case")
                    .data(null)
                    .build();
        }
        JSONObject result = caseEntity.getResult();
        if (result == null || result.isEmpty()) {
            return CommonResultVO.builder()
                    .status(1)
                    .message("该文件路径为空，该任务可能未完成或者失败")
                    .build();
        } else {
            return CommonResultVO.builder()
                    .status(1)
                    .message("文件路径获取成功")
                    .data(result)
                    .build();
        }
    }
}
