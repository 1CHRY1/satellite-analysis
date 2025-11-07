package com.ogms.dge.container.modules.method.vo;

import java.util.Map;

public class MethodParam {

    // 处理方法ID
    private String method;

    private Map<String,Object> params;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
