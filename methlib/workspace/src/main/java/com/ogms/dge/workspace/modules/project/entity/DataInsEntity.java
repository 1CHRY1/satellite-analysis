package com.ogms.dge.workspace.modules.project.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作空间数据实例
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 21:48:58
 */
@Data
@TableName("ws_data_ins")
public class DataInsEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 数据实例名称
	 */
	private String name;
	/**
	 * 唯一标识
	 */
	private String uuid;
	/**
	 * 数据服务名称
	 */
	private String serviceName;
	/**
	 * 数据服务uuid
	 */
	private String serviceUuid;
	/**
	 * 数据类型
	 */
	private String type;
	/**
	 * 数据源名称
	 */
	private String sourceName;
	/**
	 * 数据源uuid
	 */
	private String sourceUuid;
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
	 * 创建者ID
	 */
	private Long createUserId;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
