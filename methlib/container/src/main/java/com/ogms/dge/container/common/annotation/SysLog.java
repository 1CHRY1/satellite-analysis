
package com.ogms.dge.container.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义系统日志注解
 */
@Target(ElementType.METHOD) // 注解的使用范围为：方法
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时有效
@Documented // 用于指示将被注解的元素包含在生成的Java文档中
public @interface SysLog {
	// SysLog 注解只有1个参数为 value，类型为String，默认值为 “”
	String value() default "";
}
