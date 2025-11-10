package com.ogms.dge.container.modules.method.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 方法与标签对应关系
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2024-08-14 10:25:54
 */
@Data
@TableName("container_method_tag")
public class MethodTagEntity implements Serializable {
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
	 * 标签ID
	 */
	private Long tagId;

}
