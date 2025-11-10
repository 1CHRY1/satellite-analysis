package com.ogms.dge.container.modules.fs.entity.query;
import lombok.Data;

@Data
public class BaseParam {
    // 页码
    private Integer pageNo;
    // 页面大小
    private Integer pageSize;
}
