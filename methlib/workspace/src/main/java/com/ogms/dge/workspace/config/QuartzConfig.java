package com.ogms.dge.workspace.config;

import org.quartz.Scheduler;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Properties;

/**
 * @name: QuartzConfig
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/14/2024 11:31 AM
 * @version: 1.0
 */
@Configuration
public class QuartzConfig implements SchedulerFactoryBeanCustomizer {

    private final QuartzSpringJobFactory customSpringJobFactory;

    public QuartzConfig(QuartzSpringJobFactory customSpringJobFactory) {
        this.customSpringJobFactory = customSpringJobFactory;
    }

    @Bean
    public Properties properties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        // 对quartz.properties文件进行读取
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        // 在quartz.properties中的属性被读取并注入后再初始化对象
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setSchedulerName("DefaultQuartzScheduler");
        schedulerFactoryBean.setQuartzProperties(properties());
        return schedulerFactoryBean;
    }

    /*
     * quartz初始化监听器
     */
    @Bean
    public QuartzInitializerListener executorListener() {
        return new QuartzInitializerListener();
    }

    /*
     * 通过SchedulerFactoryBean获取Scheduler的实例
     */
    @Bean
    public Scheduler scheduler() throws IOException {
        Scheduler scheduler = schedulerFactoryBean().getScheduler();

        try {
            scheduler.setJobFactory(customSpringJobFactory);  // 设置自定义的 JobFactory
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scheduler;
    }

    /**
     * 使用阿里的druid作为数据库连接池
     */
    @Override
    public void customize(@NotNull SchedulerFactoryBean schedulerFactoryBean) {
        schedulerFactoryBean.setStartupDelay(2);
        schedulerFactoryBean.setAutoStartup(true);
        schedulerFactoryBean.setOverwriteExistingJobs(true);
    }
}
