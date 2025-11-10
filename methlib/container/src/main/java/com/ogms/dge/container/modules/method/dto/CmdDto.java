package com.ogms.dge.container.modules.method.dto;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CmdDto {

    public String cmd;

    public List<String> outputFileRealNameList = new ArrayList<>();

    public List<String> fileFrontNameList = new ArrayList<>();

    public List<String> newFilePidList = new ArrayList<>();

    public List<String> extensionList = new ArrayList<>();

    public List<String> inputParams = new ArrayList<>();

    public String inputFilePid = "0";

    public List<File> inputSrcFiles = new ArrayList<>();

    public List<String> inputFileIds = new ArrayList<>();

    public Boolean hasOutput = false;

    public String outputFilePath = "";

    public String tmpFilePath = "";

    // 针对invoke
    public Map<String, Object> outputFileNameList = new HashMap<>();

    public String outputFileGroup;
}
