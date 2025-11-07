package com.ogms.dge.container.modules.method.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.schema.ValidationMessage;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.modules.method.dto.CmdOutputDto;
import com.ogms.dge.container.modules.method.entity.MethodEntity;
import com.ogms.dge.container.modules.method.vo.MethodVo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 数据处理方法
 */
public interface MethodService extends IService<MethodEntity> {

    PageUtils queryPage(Map<String, Object> params) throws JsonProcessingException;

    PageUtils queryPageWithTag(Map<String, Object> params) throws JsonProcessingException;

    Map<String, Object> uploadMethod(Long userId,
            MultipartFile file,
            String fileName,
            Integer chunkIndex,
            Integer chunks);

    void saveMethod(MethodVo methodVo) throws JsonProcessingException;

    @Transactional(rollbackFor = Exception.class)
    void update(MethodVo methodVo) throws JsonProcessingException;

    @Transactional(rollbackFor = Exception.class)
    void deleteBatch(Long[] methodIds);

    @Transactional(rollbackFor = Exception.class)
    Map<String, Object> invoke(Long userId, MethodEntity method, Map<String, Object> params) throws IOException;

    @Transactional(rollbackFor = Exception.class)
    Map<String, Object> run(Long userId, MethodEntity method, String executionId, String serviceUuid, Map<String, Object> params) throws IOException;

    List<CmdOutputDto> getOutput(String executionId);

    Map<String, Object> getParamType(List<Map<String, Object>> paramSpecsList);

    List<ValidationMessage> validate(Map<String, Object> json) throws JsonProcessingException;
}
