package com.ogms.dge.container.modules.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 上传记录
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-27 14:33:42
 */
@Data
@TableName("upload_record")
public class UploadRecordEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 描述信息
	 */
	private String packageName;
	/**
	 * 所在目录文件夹
	 */
	private String packageUuid;
	/**
	 * 上传者ID
	 */
	private Long userId;
	/**
	 * 类型
	 */
	private Integer type;
	/**
	 * 状态
	 */
	private Integer status;
	/**
	 * 描述信息
	 */
	private String description;
	/**
	 * 上传时间
	 */
	private Date uploadTime;

}
