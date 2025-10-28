package nnu.mnr.satellite.config.db;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import com.alibaba.druid.filter.Filter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;

@Configuration
public class DruidConfig {

    @Bean
    public FilterRegistrationBean<jakarta.servlet.Filter> druidWebStatFilter() {
        FilterRegistrationBean<jakarta.servlet.Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new WebStatFilter());
        registrationBean.addUrlPatterns("/*");

        Map<String, String> initParams = new HashMap<>();
        initParams.put("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        initParams.put("sessionStatEnable", "true");  // 启用 Session 监控
        registrationBean.setInitParameters(initParams);

        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean<StatViewServlet> druidServlet() {
        ServletRegistrationBean<StatViewServlet> bean = new ServletRegistrationBean<>(
                new StatViewServlet(), "/druid/*");  // 必须以 /* 结尾
        return bean;
    }

    // 3. 注册 WallFilter（防火墙功能）
    @Bean
    public WallFilter wallFilter() {
        WallFilter wallFilter = new WallFilter();
        // 可选：动态加载YAML中的Wall配置（需配合@ConfigurationProperties）
        WallConfig config = new WallConfig();
        config.setMultiStatementAllow(true); // 允许一次执行多条SQL
        config.setNoneBaseStatementAllow(true); // 允许非基本语句的其他语法
        wallFilter.setConfig(config);
        return wallFilter;
    }

    // 4. 注册StatFilter（SQL监控）
    @Bean
    public StatFilter statFilter() {
        StatFilter statFilter = new StatFilter();
        statFilter.setLogSlowSql(true);
        statFilter.setSlowSqlMillis(1000);
        return statFilter;
    }
}
