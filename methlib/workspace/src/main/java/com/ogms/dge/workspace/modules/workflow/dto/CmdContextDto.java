package com.ogms.dge.workspace.modules.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.Map;

/**
 * @name: CmdContextDto
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 12/20/2024 9:35 AM
 * @version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmdContextDto {
    private StringBuilder cmdBuilder;
    private Map<String, Object> paramSpecs;
    private File wdFolder;
    private CmdDto cmdDto;
    private String dataInsUuid;
    private String dataInsBasePath;
}
