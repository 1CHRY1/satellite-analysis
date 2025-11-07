package com.ogms.dge.workspace.config;

import org.quartz.Job;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

/**
 * @name: QuartzSpringJobFactory
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/14/2024 10:06 PM
 * @version: 1.0
 */
@Component
public class QuartzSpringJobFactory extends SpringBeanJobFactory {
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected Job createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Job job = (Job) super.createJobInstance(bundle);
        // 使用 Spring 自动注入依赖
        applicationContext.getAutowireCapableBeanFactory().autowireBean(job);
        return job;
    }
}
