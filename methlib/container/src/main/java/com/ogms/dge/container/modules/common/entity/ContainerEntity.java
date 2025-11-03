package com.ogms.dge.container.modules.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2025-04-02 15:07:30
 */
@Data
@TableName("container")
public class ContainerEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 自增主键
	 */
	@TableId
	private Integer id;
	/**
	 * 账户名
	 */
	private String username;
	/**
	 * MAC地址（格式：XX:XX:XX:XX:XX:XX）
	 */
	private String mac;
	/**
	 * 服务器名称
	 */
	private String serverName;
	/**
	 * IPv4地址（如192.168.1.1）
	 */
	private String ip;
	/**
	 * 注册时间
	 */
	private Date registerTime;
	/**
	 * 最后更新时间
	 */
	private Date updateTime;

}
