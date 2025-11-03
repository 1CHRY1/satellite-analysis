package com.ogms.dge.container.modules.fs.entity.dto;

import lombok.Data;

@Data
public class DownloadFileDto {
    private String downloadCode;
    private String fileId;
    private String fileName;
    private String filePath;
}
