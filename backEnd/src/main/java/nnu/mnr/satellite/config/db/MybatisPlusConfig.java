package nnu.mnr.satellite.config.db;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.SetTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 23:35
 * @Description:
 */

@Configuration
@Slf4j
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataSource dataSource) throws SQLException {

        String dbProductName = dataSource
                .getConnection()
                .getMetaData()
                .getDatabaseProductName()
                .toLowerCase();

        DbType dbType;
        if (dbProductName.contains("postgresql")) {
            dbType = DbType.POSTGRE_SQL;
        } else if (dbProductName.contains("mysql")) {
            dbType = DbType.MYSQL;
        } else if (dbProductName.contains("oracle")) {
            dbType = DbType.ORACLE;
        } else {
            throw new IllegalStateException("Unsupported database: " + dbProductName);
        }
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        return interceptor;
    }

//    @Bean
//    public GeometryTypeHandler geometryTypeHandler() {
//        return new GeometryTypeHandler();
//    }

}
