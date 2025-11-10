package com.ogms.dge.workspace.config;

/**
 * @name: DruidConnectionProvider
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/14/2024 9:02 PM
 * @version: 1.0
 */
import com.alibaba.druid.pool.DruidDataSource;
import org.quartz.utils.ConnectionProvider;
import java.sql.Connection;
import java.sql.SQLException;

public class DruidConnectionProvider implements ConnectionProvider {
    private DruidDataSource dataSource;

    // 配置项（需要提供 setter 方法）
    private String driverClassName;
    private String url;
    private String username;
    private String password;

    // 必须提供 setter 方法来让 Quartz 设置这些属性
    public void setDriver(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public void setUser(String username) {
        this.username = username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void initialize() {
        dataSource = new DruidDataSource();

        // 通过配置设置 Druid 数据源
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() throws SQLException {
        dataSource.close();
    }
}
