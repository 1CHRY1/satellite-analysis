package nnu.mnr.satellite.config.db;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import nnu.mnr.satellite.utils.typeHandler.GeometryTypeHandler;
import nnu.mnr.satellite.utils.typeHandler.SetTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/12 23:35
 * @Description:
 */

@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 根据你的数据库类型选择
        return interceptor;
    }

//    @Bean
//    public GeometryTypeHandler geometryTypeHandler() {
//        return new GeometryTypeHandler();
//    }

}
