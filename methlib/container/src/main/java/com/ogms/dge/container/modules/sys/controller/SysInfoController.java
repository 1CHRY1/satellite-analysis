package com.ogms.dge.container.modules.sys.controller;

import com.ogms.dge.container.modules.sys.service.SysInfoService;
import com.sun.management.OperatingSystemMXBean;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.modules.method.controller.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @name: SysInfoController
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 8/12/2024 10:42 AM
 * @version: 1.0
 */
@RestController
@RequestMapping("/sys/info")
public class SysInfoController extends AbstractController {

    @Autowired
    private SysInfoService sysInfoService;

    @GetMapping("/usage")
    public R usage() throws UnknownHostException, SocketException {
        Map<String, Object> map = sysInfoService.initSystemInfo();
        return R.ok().put(map)
                .put("usageOfMemory", sysInfoService.getMemoryInfo())
                .put(sysInfoService.getHost());
    }

    @GetMapping("/host")
    public R host() throws UnknownHostException, SocketException {
        return R.ok().put(sysInfoService.getHost());
    }

}
