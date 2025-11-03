package com.ogms.dge.container.modules.sys.service;

import oshi.SystemInfo;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @name: SysInfoService
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 4/2/2025 2:36 PM
 * @version: 1.0
 */
public interface SysInfoService {
    Map<String, Object> getHost() throws UnknownHostException, SocketException;

    Map<String, Object> initSystemInfo();

    double getCpuInfo(SystemInfo systemInfo) throws InterruptedException;

    double getMemoryInfo();
}
