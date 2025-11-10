package com.ogms.dge.container.modules.job.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ogms.dge.container.common.utils.FileUtils;
import com.ogms.dge.container.modules.method.entity.MethodLogEntity;
import com.ogms.dge.container.modules.method.service.MethodLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @name: MethodLogTask
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 8/26/2024 8:25 PM
 * @version: 1.0
 */
@Component("methodLogTask")
public class MethodLogTask implements ITask {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MethodLogService methodLogService;

    @Value("${container.method.wd}")
    private String wd;

    @Override
    public void run(String params) {
        // 获取30分钟前的时间戳
        long halfHourAgo = System.currentTimeMillis() - (30 * 60 * 1000);

        try {
            // 遍历工作目录下的所有文件夹
            Files.list(Paths.get(wd))
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try {
                            // 获取最后修改时间
                            long lastModified = Files.getLastModifiedTime(dir).toMillis();
                            if (lastModified < halfHourAgo) {
                                // 删除过期目录
                                FileUtils.deleteDirectory(dir.toString());
                                System.out.println("Deleted: " + dir);
                            }
                        } catch (IOException e) {
                            System.err.println("Error processing directory: " + dir);
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error listing directories in: " + wd);
            e.printStackTrace();
        }
    }
}
