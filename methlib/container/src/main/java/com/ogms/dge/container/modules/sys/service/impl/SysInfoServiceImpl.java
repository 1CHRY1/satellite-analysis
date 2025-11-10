package com.ogms.dge.container.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.modules.sys.dao.SysLogDao;
import com.ogms.dge.container.modules.sys.entity.SysLogEntity;
import com.ogms.dge.container.modules.sys.service.SysInfoService;
import com.ogms.dge.container.modules.sys.service.SysLogService;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @name: SysInfoServiceImpl
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 4/2/2025 2:39 PM
 * @version: 1.0
 */
@Service("sysInfoService")
public class SysInfoServiceImpl implements SysInfoService {

    @Value("${server.port}")
    private int serverPort;

    @Override
    public Map<String, Object> getHost() throws UnknownHostException, SocketException {
        Map<String, Object> map = new HashMap<>();
        // -------------------- Get Host Info --------------------
        InetAddress localhost = InetAddress.getLocalHost();
        String hostname = localhost.getHostName();
        String hostIp = InetAddress.getLocalHost().getHostAddress();
        map.put("hostName", hostname);
        map.put("hostIp", hostIp);
        map.put("port", this.serverPort);
        // -------------------- Get MAC Address --------------------
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localhost);
        byte[] mac = networkInterface.getHardwareAddress();
        StringBuilder macAddress = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            macAddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        map.put("mac", macAddress.toString());
        return map;
    }

    @Override
    public Map<String, Object> initSystemInfo() {
        Map<String, Object> info = new HashMap<>();
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
        try {
            SystemInfo systemInfo = new SystemInfo();
            OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            // 椎内存使用情况
            MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();

            // 初始的总内存
            long initTotalMemorySize = memoryUsage.getInit();
            // 最大可用内存
            long maxMemorySize = memoryUsage.getMax();
            // 已使用的内存
            long usedMemorySize = memoryUsage.getUsed();

            // 操作系统
            String osName = System.getProperty("os.name");
            // 总的物理内存
            String totalMemorySize = new DecimalFormat("#.##").format(osmxb.getTotalPhysicalMemorySize() / 1024.0 / 1024 / 1024) + "G";
            // 剩余的物理内存
            String freePhysicalMemorySize = new DecimalFormat("#.##").format(osmxb.getFreePhysicalMemorySize() / 1024.0 / 1024 / 1024) + "G";
            // 已使用的物理内存
            String usedMemory = new DecimalFormat("#.##").format((osmxb.getTotalPhysicalMemorySize() - osmxb.getFreePhysicalMemorySize()) / 1024.0 / 1024 / 1024) + "G";
            // 获得线程总数
            ThreadGroup parentThread;
            for (parentThread = Thread.currentThread().getThreadGroup(); parentThread
                    .getParent() != null; parentThread = parentThread.getParent()) {

            }

            int totalThread = parentThread.activeCount();

            // 磁盘使用情况
            File[] files = File.listRoots();
            for (File file : files) {
                String total = new DecimalFormat("#.#").format(file.getTotalSpace() * 1.0 / 1024 / 1024 / 1024) + "G";
                String free = new DecimalFormat("#.#").format(file.getFreeSpace() * 1.0 / 1024 / 1024 / 1024) + "G";
                String un = new DecimalFormat("#.#").format(file.getUsableSpace() * 1.0 / 1024 / 1024 / 1024) + "G";
                String path = file.getPath();
                // System.err.println(path + "总:" + total + ",可用空间:" + un + ",空闲空间:" + free);
                // System.err.println("=============================================");
            }

            // System.err.println("操作系统:" + osName);
            info.put("operatingSystem", osName);

             /*System.err.println("程序启动时间:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ManagementFactory.getRuntimeMXBean().getStartTime())));
             System.err.println("pid:" + System.getProperty("PID"));
             System.err.println("pid:" + getPID());
             System.err.println("cpu核数:" + Runtime.getRuntime().availableProcessors());*/
            info.put("coresOfCPU", Runtime.getRuntime().availableProcessors());
            double usageOfCPU = getCpuInfo(systemInfo);
            info.put("usageOfCPU", usageOfCPU);
            /*System.err.println("JAVA_HOME:" + System.getProperty("java.home"));
            System.err.println("JAVA_VERSION:" + System.getProperty("java.version"));
            System.err.println("USER_HOME:" + System.getProperty("user.home"));
            System.err.println("USER_NAME:" + System.getProperty("user.name"));
            System.err.println("初始的总内存(JVM):" + new DecimalFormat("#.#").format(initTotalMemorySize * 1.0 / 1024 / 1024) + "M");
            System.err.println("最大可用内存(JVM):" + new DecimalFormat("#.#").format(maxMemorySize * 1.0 / 1024 / 1024) + "M");
            System.err.println("已使用的内存(JVM):" + new DecimalFormat("#.#").format(usedMemorySize * 1.0 / 1024 / 1024) + "M");
            System.err.println("总的物理内存:" + totalMemorySize);
            System.err.println("总的物理内存:" + new DecimalFormat("#.##").format(systemInfo.getHardware().getMemory().getTotal() * 1.0 / 1024 / 1024 / 1024) + "M");
            System.err.println("剩余的物理内存:" + freePhysicalMemorySize);
            System.err.println("剩余的物理内存:" + new DecimalFormat("#.##").format(systemInfo.getHardware().getMemory().getAvailable() * 1.0 / 1024 / 1024 / 1024) + "M");
            System.err.println("已使用的物理内存:" + usedMemory);
            System.err.println("已使用的物理内存:" + new DecimalFormat("#.##").format((systemInfo.getHardware().getMemory().getTotal() - systemInfo.getHardware().getMemory().getAvailable()) * 1.0 / 1024 / 1024 / 1024) + "M");
            System.err.println("总线程数:" + totalThread);
            System.err.println("===========================");*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
//        }, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public double getCpuInfo(SystemInfo systemInfo) throws InterruptedException {

        // todo 解决 Why does OSHI's System and Processor CPU usage differ from the Windows Task Manager?
//        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);

        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // 睡眠1s
        TimeUnit.SECONDS.sleep(1);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        /*System.err.println("cpu核数:" + processor.getLogicalProcessorCount());
        System.err.println("cpu系统使用率:" + new DecimalFormat("#.##%").format(cSys * 1.0 / totalCpu));
        System.err.println("cpu用户使用率:" + new DecimalFormat("#.##%").format(user * 1.0 / totalCpu));
        System.err.println("cpu当前等待率:" + new DecimalFormat("#.##%").format(iowait * 1.0 / totalCpu));
        System.err.println("cpu当前空闲率:" + new DecimalFormat("#.##%").format(idle * 1.0 / totalCpu));*/

        // todo cup利用
        //  user + system + nice + iowait + irq + softirq + steal
        long cpuUtilization = user + nice + cSys + iowait + irq + softirq + steal;
        // System.err.println("cpu利用率：" + new DecimalFormat("#.##%").format(cpuUtilization * 1.0 / totalCpu));
        return cpuUtilization * 1.0 / totalCpu;
    }

    @Override
    public double getMemoryInfo() {
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long totalByte = memory.getTotal();
        long acaliableByte = memory.getAvailable();
        return (totalByte - acaliableByte) * 1.0 / totalByte;
    }
}
