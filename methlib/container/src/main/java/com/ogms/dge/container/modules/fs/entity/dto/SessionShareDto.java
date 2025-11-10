package com.ogms.dge.container.modules.fs.entity.dto;

import lombok.Data;

import java.util.Date;

@Data
public class SessionShareDto {
    private String shareId;
    private Long shareUserId;
    private Date expireTime;
    private String fileId;
}
