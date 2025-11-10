package com.ogms.dge.workspace.modules.project.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-08 20:54:31
 */
@Data
@TableName("ws_project")
public class ProjectEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@TableId
	private Long id;
	/**
	 * 项目名称
	 */
	private String name;
	/**
	 * 唯一标识
	 */
	private String uuid;
	/**
	 * 唯一标识
	 */
	private String workspaceUuid;
	/**
	 * 工作流定义数
	 */
	private Integer workflowNum;
	/**
	 * 正在运行的流程数
	 */
	private Integer workflowRunningNum;
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
	/**
	 * 更新时间
	 */
	private Date updateTime;

}
