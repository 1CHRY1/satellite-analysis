package com.ogms.dge.workspace.modules.workspace.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作空间
 * 
 * @author ${author}
 * @email ${email}
 * @date 2024-11-02 14:41:49
 */
@Data
@TableName("ws")
public class WsEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 工作空间名称
	 */
	private String name;
	/**
	 * 状态
	 */
	private Integer status;
	/**
	 * 唯一标识
	 */
	private String uuid;
	/**
	 * 配置信息
	 */
	private String config;
	/**
	 * 描述信息
	 */
	private String description;
	/**
	 * 创建者ID
	 */
	private Long createUserId;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/** * 更新时间
	 */
	private Date updateTime;


}
