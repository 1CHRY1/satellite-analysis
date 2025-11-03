
package com.ogms.dge.container.modules.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 登录用户信息
 */
@Target(ElementType.PARAMETER) // 作用于参数中
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

}
