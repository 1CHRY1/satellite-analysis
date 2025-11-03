package com.ogms.dge.workspace.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @name: HttpUtils
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/19/2024 5:10 PM
 * @version: 1.0
 */
@Component
public class HttpUtils {

    @Resource
    private RestTemplate restTemplate;

    public ResponseEntity<Map> get(String url) {
        // 设置请求头，添加 token 和 Accept
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("token", "883ada2fc996ab9487bed7a3ba21d2f1");

        // 创建请求实体，包含 headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 发送请求并解析响应
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response;
    }

    public ResponseEntity<Map> get(String url, HttpHeaders headers) {
        // 创建请求实体，包含 headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 发送请求并解析响应
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response;
    }
}
