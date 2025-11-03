package com.ogms.dge.workspace.modules.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-11-13 16:02:54
 */
@Data
@TableName("ws_task")
public class TaskEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@TableId
	private Long id;
	/**
	 * 任务名称
	 */
	private String name;
	/**
	 * 任务类型
	 */
	private Integer type;
	/**
	 * 任务状态
	 */
	private Integer status;
	/**
	 * 唯一标识
	 */
	private String uuid;
	/**
	 * 所属工作流唯一标识
	 */
	private String workflowUuid;
	/**
	 * jobKey
	 */
	private String jobKey;
	/**
	 * 配置信息
	 */
	private String config;
	/**
	 * 描述信息
	 */
	private String description;
	/**
	 * 日志信息
	 */
	private String log;
	/**
	 * 创建者ID
	 */
	private Long createUserId;
	/**
	 * 开始时间
	 */
	private Date startTime;
	/**
	 * 结束时间
	 */
	private Date endTime;

}
