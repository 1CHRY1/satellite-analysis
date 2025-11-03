package com.ogms.dge.container.modules.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据源类型
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-23 15:23:47
 */
@Data
@TableName("data_source")
public class DataSourceEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 数据源名称
	 */
	private String name;
	/**
	 * 数据源分类
	 */
	private String classification;
	/**
	 * 数据源标签分类
	 */
	private String classificationLabel;
	/**
	 * 元数据信息
	 */
	private String jsonSchema;

}
