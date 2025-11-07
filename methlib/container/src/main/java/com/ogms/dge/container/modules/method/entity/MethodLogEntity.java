package com.ogms.dge.container.modules.method.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 方法调用记录表
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-08-23 20:06:48
 */
@Data
@TableName("container_method_log")
public class MethodLogEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 方法ID
	 */
	private Long methodId;
	/**
	 * 调用者ID
	 */
	private Long userId;
	/**
	 * 工作目录
	 */
	private String workingDir;
	/**
	 * 输入文件id列表
	 */
	private String inputFiles;
	/**
	 * 输入类型，0是个人空间，1是数据服务映射
	 */
	private Integer inputType;
	/**
	 * 输出文件id列表
	 */
	private String outputFiles;
	/**
	 * 执行结果标识
	 */
	private Integer status;
	/**
	 * 方法输出信息
	 */
	private String info;
	/**
	 * 开始时间
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "Asia/Shanghai")
	private Date startTime;
	/**
	 * 结束时间
	 */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "Asia/Shanghai")
	private Date endTime;

}
