package com.ogms.dge.container.modules.method.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @name: CmdOutputDto
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 2/21/2025 2:46 PM
 * @version: 1.0
 */
@Data
@Builder
public class CmdOutputDto {
    private String id;          // 执行ID
    private String content;     // 输出内容
    private boolean finished;   // 是否完成
    private boolean error;    // 是否错误
    private LocalDateTime timestamp;
}
