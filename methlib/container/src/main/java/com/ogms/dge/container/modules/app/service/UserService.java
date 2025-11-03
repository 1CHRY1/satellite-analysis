package com.ogms.dge.container.modules.app.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.modules.app.entity.UserEntity;
import com.ogms.dge.container.modules.app.form.LoginForm;

/**
 * 用户
 */
public interface UserService extends IService<UserEntity> {

	UserEntity queryByMobile(String mobile);

	/**
	 * 用户登录
	 * @param form    登录表单
	 * @return        返回用户ID
	 */
	long login(LoginForm form);
}
