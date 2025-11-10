package com.ogms.dge.container.modules.fs.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 分享信息
 * </p>
 *
 * @author jin wang
 * @since 2023-09-07
 */
@Data
@TableName("file_share")
public class FileShare implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分享ID
     */
    private String shareId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 有效期类型 0:1天 1:7天 2:30天 3:永久有效
     */
    private Integer validType;

    /**
     * 失效时间, NULL 表示永久 不失效
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /**
     * 分享时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shareTime;

    /**
     * 提取码
     */
    private String code;

    /**
     * 浏览次数
     */
    private Integer showCount;
}
