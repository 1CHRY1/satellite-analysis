package com.ogms.dge.container.modules.method.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @name: MethodVo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 8/23/2024 10:48 AM
 * @version: 1.0
 */
@Data
public class MethodVo {
    /**
     *
     */
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
    private List<Map<String, Object>> params;
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
     * 标签ID列表
     */
    private List<Long> tagIdList;
    /**
     * 创建者ID
     */
    private Long createUserId;
    /**
     * 创建时间
     */
    private Date createTime;
}
