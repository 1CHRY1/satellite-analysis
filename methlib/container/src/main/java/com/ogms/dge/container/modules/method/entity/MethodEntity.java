package com.ogms.dge.container.modules.method.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数据处理方法
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2024-08-13 18:45:08
 */
@Data
@TableName("container_method")
public class MethodEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@TableId
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
