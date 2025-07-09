package nnu.mnr.satellite;

import nnu.mnr.satellite.service.common.LocalDockerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 本地Docker服务测试
 * 用于验证LocalDockerService的基本功能
 */
@SpringBootTest
@ActiveProfiles("zzw")  // 使用zzw配置文件
public class LocalDockerServiceTest {

    @Test
    public void testDockerClientCreation() {
        // 这是一个基本的Spring Boot应用启动测试
        // 如果LocalDockerService能够正常初始化，说明Docker客户端创建成功
        System.out.println("LocalDockerService test - Spring Boot application started successfully");
    }
} 