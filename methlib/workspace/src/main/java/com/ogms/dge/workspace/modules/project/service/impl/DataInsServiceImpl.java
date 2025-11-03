package com.ogms.dge.workspace.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.workspace.common.utils.FileUtils;
import com.ogms.dge.workspace.common.utils.HttpUtils;
import com.ogms.dge.workspace.common.utils.PageUtils;
import com.ogms.dge.workspace.common.utils.Query;
import com.ogms.dge.workspace.modules.project.dao.DataInsDao;
import com.ogms.dge.workspace.modules.project.entity.DataInsEntity;
import com.ogms.dge.workspace.modules.project.service.DataInsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;


@Service("dataInsService")
public class DataInsServiceImpl extends ServiceImpl<DataInsDao, DataInsEntity> implements DataInsService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${container.data.dsd}")
    private String data_dsd;

    @Value("${workspace.data.insd}")
    private String data_insd;

    @Value("${container.api}")
    private String container_api;

    @Resource
    private HttpUtils httpUtils;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String dataName = (String) params.get("key");
        IPage<DataInsEntity> page = this.page(
                new Query<DataInsEntity>().getPage(params),
                new QueryWrapper<DataInsEntity>()
                        .like(StringUtils.isNotBlank(dataName), "name", dataName)
        );

        return new PageUtils(page);
    }

    @Override
    public void sync(DataInsEntity dataIns) throws IOException {
        // 发送请求并解析响应
        ResponseEntity<Map> response = httpUtils.get(container_api + "data/service/info/uuid/" + dataIns.getServiceUuid());

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println(response.getBody());
            Map<String, Object> service = (Map<String, Object>) response.getBody().get("service");
            if (dataIns.getCreateTime() == null) {
                dataIns.setCreateTime(new Date());
            }
            dataIns.setDescription((String) service.get("description"));
            dataIns.setServiceName((String) service.get("name"));
            dataIns.setSourceName((String) service.get("srcName"));
            dataIns.setSourceUuid((String) service.get("srcUuid"));
            dataIns.setType((String) service.get("type"));
            dataIns.setUpdateTime(new Date());
            dataIns.setUuid(UUID.randomUUID().toString());
            saveOrUpdate(dataIns);
            Map<String, Object> config = (Map<String, Object>) service.get("configMap");
            if (config != null) {
                if (config.get("File") != null) {
                    String baseUrl = "";
                    List<String> filePathList = (List<String>) config.get("File");
                    // 发送请求并解析响应
                    response = httpUtils.get(container_api + "data/source/info/uuid/" + dataIns.getSourceUuid());
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Map<String, Object> source = (Map<String, Object>) response.getBody().get("source");
                        if ((Boolean) source.get("isLocalPath")) {
                            baseUrl = (String) source.get("localPath");
                        } else {
                            baseUrl = new File(data_dsd, dataIns.getSourceUuid()).getAbsolutePath();
                        }
                        List files = new ArrayList();
                        for (String path : filePathList) {
                            File file = new File(baseUrl, path);
                            files.add(file);
                            if (!file.exists()) {
                                System.out.println("File Not Found");
                                continue;
                            } else if (file.isDirectory()) {
                            }
                        }
                        FileUtils.copyFilesToDirectory(files, new File(data_insd, dataIns.getUuid()).getAbsolutePath());
                    } else {
                        throw new RuntimeException("Failed to fetch source info, status code: " + response.getStatusCode());
                    }
                }
            }
        } else {
            throw new RuntimeException("Failed to fetch service info, status code: " + response.getStatusCode());
        }
    }
}