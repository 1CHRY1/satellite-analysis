package com.ogms.dge.container.common.utils;

import com.ogms.dge.container.common.validator.group.AliyunGroup;
import com.ogms.dge.container.common.validator.group.QcloudGroup;
import com.ogms.dge.container.common.validator.group.QiniuGroup;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 常量
 */
public class Constant {
    /**
     * 超级管理员ID
     */
    public static final int SUPER_ADMIN = 1;
    /**
     * 当前页码
     */
    public static final String PAGE = "page";
    /**
     * 每页显示记录数
     */
    public static final String LIMIT = "limit";
    /**
     * 排序字段
     */
    public static final String ORDER_FIELD = "sidx";
    /**
     * 排序方式
     */
    public static final String ORDER = "order";
    /**
     * 升序
     */
    public static final String ASC = "asc";

    /**
     * 菜单类型
     *
     * @author chenshun
     * @email sunlightcs@gmail.com
     * @date 2016年11月15日 下午1:24:29
     */
    public enum MenuType {
        /**
         * 目录
         */
        CATALOG(0),
        /**
         * 菜单
         */
        MENU(1),
        /**
         * 按钮
         */
        BUTTON(2);

        private int value;

        MenuType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 定时任务状态
     *
     * @author chenshun
     * @email sunlightcs@gmail.com
     * @date 2016年12月3日 上午12:07:22
     */
    public enum ScheduleStatus {
        /**
         * 正常
         */
        NORMAL(0),
        /**
         * 暂停
         */
        PAUSE(1);

        private int value;

        ScheduleStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 云服务商
     */
    public enum CloudService {
        /**
         * 七牛云
         */
        QINIU(1, QiniuGroup.class),
        /**
         * 阿里云
         */
        ALIYUN(2, AliyunGroup.class),
        /**
         * 腾讯云
         */
        QCLOUD(3, QcloudGroup.class);

        private int value;

        private Class<?> validatorGroupClass;

        CloudService(int value, Class<?> validatorGroupClass) {
            this.value = value;
            this.validatorGroupClass = validatorGroupClass;
        }

        public int getValue() {
            return value;
        }

        public Class<?> getValidatorGroupClass() {
            return this.validatorGroupClass;
        }

        public static CloudService getByValue(Integer value) {
            Optional<CloudService> first = Stream.of(CloudService.values()).filter(cs -> value.equals(cs.value)).findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("非法的枚举值:" + value);
            }
            return first.get();
        }
    }


    // Attention!  以下为云盘的字段：
    public static final String ZERO_STR = "0";

    public static final Integer ZERO = 0;

    public static final Integer ONE = 1;

    public static final Integer LENGTH_30 = 30;

    public static final Integer LENGTH_10 = 10;

    public static final Integer LENGTH_20 = 20;

    public static final Integer LENGTH_5 = 5;

    public static final Integer LENGTH_15 = 15;

    public static final Integer LENGTH_150 = 150;

    public static final Integer LENGTH_50 = 50;

    public static final String SESSION_KEY = "session_key";

    public static final String SESSION_SHARE_KEY = "session_share_key_";

    public static final String FILE_FOLDER_FILE = "/file/";

    public static final String FILE_FOLDER_TEMP = "/temp/";

    public static final String IMAGE_PNG_SUFFIX = ".png";

    public static final String TS_NAME = "index.ts";

    public static final String M3U8_NAME = "index.m3u8";

    public static final String CHECK_CODE_KEY = "check_code_key";

    public static final String CHECK_CODE_KEY_EMAIL = "check_code_key_email";

    public static final String AVATAR_SUFFIX = ".jpg";

    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";

    public static final String AVATAR_DEFUALT = "default_avatar.jpg";

    public static final String VIEW_OBJ_RESULT_KEY = "result";

    /**
     * redis key 相关
     */

    /**
     * 过期时间 1分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60;

    /**
     * 过期时间 1天
     */
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;

    public static final Integer REDIS_KEY_EXPIRES_ONE_HOUR = REDIS_KEY_EXPIRES_ONE_MIN * 60;

    public static final Long MB = 1024 * 1024L;

    /**
     * 过期时间5分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_FIVE_MIN = REDIS_KEY_EXPIRES_ONE_MIN * 5;


    public static final String REDIS_KEY_DOWNLOAD = "dsc:download:";

    public static final String REDIS_KEY_SYS_SETTING = "dsc:syssetting:";

    public static final String REDIS_KEY_USER_SPACE_USE = "dsc:user:spaceuse:";

    public static final String REDIS_KEY_USER_FILE_TEMP_SIZE = "dsc:user:file:temp:";

}
