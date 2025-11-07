package com.ogms.dge.workspace.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.common.utils.HttpUtils;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.Query;
import com.ogms.dge.workspace.modules.project.dao.MethodInsDao;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;
import com.ogms.dge.workspace.modules.project.entity.MethodInsEntity;
import com.ogms.dge.workspace.modules.project.service.MethodInsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;


@Service("methodInsService")
public class MethodInsServiceImpl extends ServiceImpl<MethodInsDao, MethodInsEntity> implements MethodInsService {

    @Value("${container.method.pd}")
    private String method_pd;

    @Value("${workspace.method.insd}")
    private String method_insd;

    @Value("${container.api}")
    private String container_api;

    @Resource
    private HttpUtils httpUtils;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String methodName = (String) params.get("key");
        String methodType = (String) params.get("type");
        IPage<MethodInsEntity> page = this.page(
                new Query<MethodInsEntity>().getPage(params),
                new QueryWrapper<MethodInsEntity>()
                        .like(StringUtils.isNotBlank(methodName), "name", methodName)
                        .eq(StringUtils.isNotBlank(methodType), "type", methodType)
        );

        return new PageUtils(page);
    }

    @Override
    public void sync(MethodInsEntity methodIns) throws IOException {
        // 发送请求并解析响应
        ResponseEntity<Map> response = httpUtils.get(container_api + "method/info/" + methodIns.getMethodId());

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println(response.getBody());
            Map<String, Object> method = (Map<String, Object>) response.getBody().get("method");
            if (methodIns.getCreateTime() == null) {
                methodIns.setCreateTime(new Date());
            }
            methodIns.setDescription((String) method.get("description"));
            methodIns.setMethodName((String) method.get("name"));
            methodIns.setType((String) method.get("type"));
            methodIns.setUuid(UUID.randomUUID().toString());
            saveOrUpdate(methodIns);
            String baseUrl = method_pd;
            File parentPath = new File(baseUrl, (String) method.get("uuid"));
            List<File> files = FileUtils.getAllFilesFromDirectory(parentPath.getAbsolutePath());
            FileUtils.copyFilesToDirectory(files, new File(method_insd, methodIns.getUuid()).getAbsolutePath());
        } else {
            throw new RuntimeException("Failed to fetch method info, status code: " + response.getStatusCode());
        }
    }

}