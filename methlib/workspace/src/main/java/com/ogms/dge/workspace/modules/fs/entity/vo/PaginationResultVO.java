package com.ogms.dge.workspace.modules.fs.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("创建Swagger请求参数")
public class PaginationResultVO<T> {
    @ApiModelProperty("总数")
    private Long totalCount;

    @ApiModelProperty("每页请求数")
    private Integer pageSize;

    @ApiModelProperty("页码")
    private Integer pageNo;

    @ApiModelProperty("总页数")
    private Long pageTotal;

    @ApiModelProperty("数据列表")
    private List<T> list = new ArrayList<T>();
}
