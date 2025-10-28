package nnu.mnr.satellite.service.admin;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

@SpringBootTest
public class SmartApplicationTest {

    @Autowired
    private DataSource dataSource ;

    @Test
    public void contextLoads() {
        System.out.println(dataSource.getClass());
        DruidDataSource druidDataSource = (DruidDataSource) dataSource;
        System.out.println("druidDataSource.getUrl() = " + druidDataSource.getUrl());
        System.out.println("druidDataSource.getUsername() = " + druidDataSource.getUsername());
        System.out.println("druidDataSource.getInitialSize() = " + druidDataSource.getInitialSize());
        System.out.println("druidDataSource.getMaxActive() = " + druidDataSource.getMaxActive());
    }

}