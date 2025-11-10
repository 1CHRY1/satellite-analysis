package com.ogms.dge.workspace.modules.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-19 15:19:51
 */
@Data
@TableName("ws_workflow")
public class WorkflowEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@TableId
	private Long id;
	/**
	 * 工作流案例名称
	 */
	private String name;
	/**
	 * 唯一标识
	 */
	private String uuid;
	/**
	 * 项目uuid
	 */
	private String projectUuid;
	/**
	 * 创建时间
	 */
	private Date createTime;

}
