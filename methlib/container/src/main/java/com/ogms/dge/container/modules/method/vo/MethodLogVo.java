package com.ogms.dge.container.modules.method.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @name: MethodLogVo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 9/10/2024 8:50 PM
 * @version: 1.0
 */
@Data
public class MethodLogVo {
    private Long id;
    /**
     * 方法ID
     */
    private Long methodId;
    /**
     * 调用者ID
     */
    private Long userId;
    /**
     * 工作目录
     */
    private String workingDir;
    /**
     * 输入文件id列表
     */
    private List<String> inputFiles;
    /**
     * 输入文件id列表
     */
    private Integer inputType;
    /**
     * 输出文件id列表
     */
    private List<String> outputFiles;
    /**
     * 执行结果标识
     */
    private Integer status;
    /**
     * 方法输出信息
     */
    private String info;
    /**
     * 开始时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "Asia/Shanghai")
    private Date startTime;
    /**
     * 结束时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "Asia/Shanghai")
    private Date endTime;
}
