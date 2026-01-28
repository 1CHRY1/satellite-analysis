package nnu.mnr.satellite;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;

@SpringBootTest
class BackEndApplicationTests {

    @Autowired
    private DataSource dataSource ;

    @Test
    void contextLoads() throws Exception {
        System.out.println("=====================================");
        System.out.println("DataSource class: " + dataSource.getClass());

        if (dataSource instanceof DynamicRoutingDataSource) {
            DynamicRoutingDataSource dynamicDataSource = (DynamicRoutingDataSource) dataSource;
            Map<String, DataSource> dataSources = dynamicDataSource.getDataSources();
            DataSource itemDataSource = dataSources.get("pg-ard-satellite");
            System.out.println("ItemDataSource class: " + itemDataSource.getClass());

            // 反射获取 ItemDataSource 内部的真实 DataSource
            Field dataSourceField = itemDataSource.getClass().getDeclaredField("dataSource");
            dataSourceField.setAccessible(true);
            Object realDataSource = dataSourceField.get(itemDataSource);
            System.out.println("Real DataSource class: " + realDataSource.getClass());

            // 如果是 DruidDataSource，可以强制转换并获取连接信息
            if (realDataSource instanceof com.alibaba.druid.pool.DruidDataSource) {
                com.alibaba.druid.pool.DruidDataSource druidDataSource = (com.alibaba.druid.pool.DruidDataSource) realDataSource;
                System.out.println("Druid URL: " + druidDataSource.getUrl());
            }
        }
    }

}
