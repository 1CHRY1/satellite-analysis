package com.ogms.dge.workspace.modules.workflow.dto;

import lombok.Data;

import java.util.Date;

/**
 * @name: MethodDto
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/19/2024 5:02 PM
 * @version: 1.0
 */
@Data
public class MethodDto {
    private Long id;
    /**
     * 方法名称
     */
    private String name;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 详细描述信息
     */
    private String longDesc;
    /**
     * 版权信息
     */
    private String copyright;
    /**
     * 条目
     */
    private String category;
    /**
     * 唯一标识
     */
    private String uuid;
    /**
     * 类型
     */
    private String type;
    /**
     * 参数信息
     */
    private String params;
    /**
     * 执行方式
     */
    private String execution;
    /**
     * 输入UDX
     */
    private String inputSchema;
    /**
     * 输出UDX
     */
    private String outputSchema;
    /**
     * 创建者ID
     */
    private Long createUserId;
    /**
     * 创建时间
     */
    private Date createTime;
}
