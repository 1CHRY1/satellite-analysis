package com.ogms.dge.container.config;

import com.ogms.dge.container.modules.sys.jwt.JWTFilter;
import com.ogms.dge.container.modules.sys.jwt.JWTRealm;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro配置
 */
@Configuration
public class ShiroConfig {

    @Bean("securityManager")
    public SecurityManager securityManager(JWTRealm jwtRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(jwtRealm);
        securityManager.setRememberMeManager(null);
        return securityManager;
    }

    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);

        //jwt token过滤
        Map<String, Filter> filters = new HashMap<>();
        filters.put("jwt", new JWTFilter());
        shiroFilter.setFilters(filters);

        Map<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("/webjars/**", "anon");
        filterMap.put("/druid/**", "anon");
        filterMap.put("/app/**", "anon");
        filterMap.put("/sys/login", "anon");
        filterMap.put("/swagger/**", "anon");
        filterMap.put("/v2/api-docs", "anon");
        filterMap.put("/swagger-ui.html", "anon");
        filterMap.put("/swagger-resources/**", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/aaa.txt", "anon");
        // 暂时为以下添加鉴权，以进行用户识别
        filterMap.put("/file/createDownloadUrl/**", "jwt");
        filterMap.put("/method/**", "jwt");
        filterMap.put("/method/save", "jwt");
        filterMap.put("/method/createUUID", "jwt");
        filterMap.put("/tag/**", "jwt");
        filterMap.put("/common/**", "jwt");
        filterMap.put("/data/**", "jwt");
        filterMap.put("/sys/**", "jwt");
        filterMap.put("/file/**", "jwt");
        // 设置下载路径不受鉴权控制，用Redis有效时长进行控制
        filterMap.put("/file/download/**", "anon");
        filterMap.put("/data/service/download/**", "anon");
        // 暂时屏蔽其他所有接口的鉴权
        //filterMap.put("/**", "jwt");
        shiroFilter.setFilterChainDefinitionMap(filterMap);

        return shiroFilter;
    }

    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

}
