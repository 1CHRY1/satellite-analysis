package com.ogms.dge.workspace.modules.project.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作空间方法实例
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:32:37
 */
@Data
@TableName("ws_method_ins")
public class MethodInsEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 方法实例名称
	 */
	private String name;
	/**
	 * 唯一标识
	 */
	private String uuid;
	/**
	 * 数据处理服务名称
	 */
	private String methodName;
	/**
	 * 方法ID
	 */
	private Long methodId;
	/**
	 * 方法类型
	 */
	private String type;
	/**
	 * 项目uuid
	 */
	private String projectUuid;
	/**
	 * 描述信息
	 */
	private String description;
	/**
	 * 组ID
	 */
	private Long groupId;
	/**
	 * 创建时间
	 */
	private Date createTime;

}
