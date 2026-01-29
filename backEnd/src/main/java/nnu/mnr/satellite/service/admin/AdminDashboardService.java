package nnu.mnr.satellite.service.admin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.minio.admin.messages.info.Message;
import io.minio.admin.messages.info.ServerProperties;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.enums.common.SceneTypeByResolution;
import nnu.mnr.satellite.enums.common.SceneTypeByTheme;
import nnu.mnr.satellite.mapper.resources.ICaseRepo;
import nnu.mnr.satellite.mapper.resources.ISceneRepoV3;
import nnu.mnr.satellite.mapper.resources.IVectorRepo;
import nnu.mnr.satellite.mapper.user.IUserRepo;
import nnu.mnr.satellite.model.po.resources.Case;
import nnu.mnr.satellite.model.po.resources.Vector;
import nnu.mnr.satellite.model.vo.admin.SceneSimpleInfoVO;
import nnu.mnr.satellite.model.vo.admin.StatsReportVO;
import nnu.mnr.satellite.model.vo.admin.SystemInfoReportVO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.resources.SceneDataServiceV3;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminDashboardService {

    @Autowired
    private ICaseRepo caseRepo;
    @Autowired
    private ISceneRepoV3 sceneRepo;
    @Autowired
    private IVectorRepo vectorRepo;
    @Autowired
    private IUserRepo userRepo;
    @Autowired
    private SceneDataServiceV3 sceneDataService;
    @Autowired
    private MinioUtil minioUtil;

    public CommonResultVO getStats() { // 注意：建议去掉 throws，在内部消化异常
        StatsReportVO statsReportVO = new StatsReportVO();

        // 1. 获取 Overall 数据 (数据库操作，通常较稳定)
        try {
            Long sceneCount = sceneRepo.selectCount(null);
            Long caseCount = caseRepo.selectCount(null);
            Long userCount = userRepo.selectCount(null);
            StatsReportVO.OverallVO overallVO = StatsReportVO.OverallVO.builder()
                    .data(sceneCount)
                    .task(caseCount)
                    .user(userCount)
                    .build();
            statsReportVO.setOverall(overallVO);
        } catch (Exception e) {
            log.error("获取总体统计失败", e);
            statsReportVO.setOverall(StatsReportVO.OverallVO.builder().build()); // 失败给空对象
        }

        // 2. 获取 Data 分类统计 (包含你之前的逻辑)
        try {
            List<SceneSimpleInfoVO> allScenes = sceneRepo.getAllScenes();
            List<SceneSimpleInfoVO> scenes = new ArrayList<>();
            List<SceneSimpleInfoVO> themes = new ArrayList<>();
            for (SceneSimpleInfoVO scene : allScenes) {
                String sceneDataType = scene.getDataType();
                if ("satellite".equals(sceneDataType)) {
                    scenes.add(scene);
                } else if (SceneTypeByTheme.getAllCodes().contains(sceneDataType)) {
                    themes.add(scene);
                }
            }

            // data.satellite
            List<StatsReportVO.DataVO.Info> satelliteList = new ArrayList<>();
            for (SceneTypeByResolution type : SceneTypeByResolution.values()) {
                StatsReportVO.DataVO.Info info = new StatsReportVO.DataVO.Info();
                info.setKey(type.name());
                info.setLabel(type.getLabel());
                // 【注意】这里使用了之前优化的判空逻辑
                List<SceneSimpleInfoVO> filteredScenes = scenes.stream()
                        .filter(scene -> isSceneMatchResolutionType(scene, type))
                        .toList();
                info.setValue((long) filteredScenes.size());
                satelliteList.add(info);
            }

            // data.theme
            List<StatsReportVO.DataVO.Info> themeList = new ArrayList<>();
            for (SceneTypeByTheme type : SceneTypeByTheme.values()) {
                StatsReportVO.DataVO.Info info = new StatsReportVO.DataVO.Info();
                info.setKey(type.getCode());
                info.setLabel(type.getLabel());
                List<SceneSimpleInfoVO> filteredThemes = themes.stream()
                        .filter(theme -> type.getCode().equalsIgnoreCase(theme.getDataType()))
                        .toList();
                info.setValue((long) filteredThemes.size());
                themeList.add(info);
            }

            // data.vector
            List<StatsReportVO.DataVO.Info> vectorList = new ArrayList<>();
            List<Vector> vectors = vectorRepo.selectList(null);
            for (Vector vector : vectors) {
                StatsReportVO.DataVO.Info info = new StatsReportVO.DataVO.Info();
                info.setKey(vector.getTableName());
                info.setLabel(vector.getVectorName());
                info.setValue((long) vector.getCount());
                vectorList.add(info);
            }

            StatsReportVO.DataVO dataVO = StatsReportVO.DataVO.builder()
                    .satelliteList(satelliteList)
                    .themeList(themeList)
                    .vectorList(vectorList)
                    .build();
            statsReportVO.setData(dataVO);
        } catch (Exception e) {
            log.error("获取数据分类统计失败", e);
            // 这里可以设置一个空的 DataVO 防止前端空指针
            statsReportVO.setData(StatsReportVO.DataVO.builder()
                    .satelliteList(new ArrayList<>())
                    .themeList(new ArrayList<>())
                    .vectorList(new ArrayList<>())
                    .build());
        }

        // 3. 获取 Task 统计
        try {
            QueryWrapper<Case> queryWrapperComplete = new QueryWrapper<>();
            queryWrapperComplete.eq("status", "COMPLETE");
            Long caseCountComplete = caseRepo.selectCount(queryWrapperComplete);

            QueryWrapper<Case> queryWrapperError = new QueryWrapper<>();
            queryWrapperError.eq("status", "ERROR");
            Long caseCountError = caseRepo.selectCount(queryWrapperError);

            QueryWrapper<Case> queryWrapperRunning = new QueryWrapper<>();
            queryWrapperRunning.eq("status", "RUNNING");
            Long caseCountRunning = caseRepo.selectCount(queryWrapperRunning);

            // 注意：taskVO.total 应该取所有任务总数，可以用之前的 caseCount 变量
            // 但为了解耦，这里重新计算或使用成员变量，假设用 caseRepo.selectCount(null)
            Long total = caseRepo.selectCount(null);

            StatsReportVO.TaskVO taskVO = StatsReportVO.TaskVO.builder()
                    .total(total)
                    .completed(caseCountComplete)
                    .error(caseCountError)
                    .running(caseCountRunning)
                    .build();
            statsReportVO.setTask(taskVO);
        } catch (Exception e) {
            log.error("获取任务统计失败", e);
            statsReportVO.setTask(StatsReportVO.TaskVO.builder().build());
        }

        // 4. 【关键修改】获取 Storage (MinIO) 信息 - 增加 Try-Catch 隔离
        try {
            // 这两行代码就是导致报错的源头
            JSONObject serverStorageInfo = minioUtil.getServerStorageInfo();
            List<JSONObject> bucketsInfoObj = minioUtil.getBucketsInfo();

            List<StatsReportVO.StorageVO.BucketInfo> bucketsInfo = new ArrayList<>();
            if (bucketsInfoObj != null) {
                bucketsInfoObj.forEach(bucketInfoObj -> {
                    StatsReportVO.StorageVO.BucketInfo bucketInfo = StatsReportVO.StorageVO.BucketInfo.builder()
                            .bucketName(bucketInfoObj.getString("bucketName"))
                            .bucketUsedSize(bucketInfoObj.getLong("bucketUsedSize"))
                            .objectsCount(bucketInfoObj.getLong("objectsCount"))
                            .build();
                    bucketsInfo.add(bucketInfo);
                });
            }

            StatsReportVO.StorageVO storageVO = StatsReportVO.StorageVO.builder()
                    .totalSpace(serverStorageInfo != null ? serverStorageInfo.getBigDecimal("totalSpace") : java.math.BigDecimal.ZERO)
                    .availSpace(serverStorageInfo != null ? serverStorageInfo.getBigDecimal("availSpace") : java.math.BigDecimal.ZERO)
                    .usedSpace(serverStorageInfo != null ? serverStorageInfo.getBigDecimal("usedSpace") : java.math.BigDecimal.ZERO)
                    .bucketsInfo(bucketsInfo)
                    .build();
            statsReportVO.setStorage(storageVO);

        } catch (Exception e) {
            // 【重点】在这里捕获那个 Unrecognized field 异常
            // 这样虽然存储信息看不到，但至少前面的 场景、任务、用户 数据能正常展示！
            log.error("MinIO 存储信息获取失败 (可能是版本不兼容): {}", e.getMessage());

            // 返回一个空的 StorageVO，或者显示 "N/A"
            StatsReportVO.StorageVO emptyStorage = StatsReportVO.StorageVO.builder()
                    .totalSpace(java.math.BigDecimal.ZERO)
                    .availSpace(java.math.BigDecimal.ZERO)
                    .usedSpace(java.math.BigDecimal.ZERO)
                    .bucketsInfo(new ArrayList<>())
                    .build();
            statsReportVO.setStorage(emptyStorage);
        }

        return CommonResultVO.builder()
                .status(1)
                .message("统计信息获取成功") // 即使部分失败，整体接口依然返回 200 OK
                .data(statsReportVO)
                .build();
    }

    public boolean isSceneMatchResolutionType(SceneSimpleInfoVO scene, SceneTypeByResolution type) {
        double sceneResolution = sceneDataService.parseResolutionToMeters(scene.getResolution());
        return switch (type) {
            case subMeter -> sceneResolution <= 1; // 亚米分辨率（≤1）
            case twoMeter -> sceneResolution > 1 && sceneResolution <= 2.0;
            case tenMeter -> sceneResolution > 2.0 && sceneResolution <= 10.0;
            case thirtyMeter -> sceneResolution > 10.0 && sceneResolution <= 30.0;
            case other -> sceneResolution > 30.0; // 其他更高分辨率
            default -> false;
        };
    }

    public CommonResultVO getActivity() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        // CPU
        JSONObject cpuInfo = new JSONObject();
        CentralProcessor processor = hardware.getProcessor();
        String cpu = processor.getProcessorIdentifier().getName();
        Integer physicalProcessorCount = processor.getPhysicalProcessorCount();
        Integer logicalProcessorCount = processor.getLogicalProcessorCount();
        double cpuUsage = processor.getSystemCpuLoad(500);
        cpuInfo.put("cpu", cpu);
        cpuInfo.put("physicalProcessorCount", physicalProcessorCount);
        cpuInfo.put("logicalProcessorCount", logicalProcessorCount);
        cpuInfo.put("cpuUsage", cpuUsage);
        // 内存
        JSONObject memoryInfo = new JSONObject();
        GlobalMemory globalMemory = hardware.getMemory();
        long totalMemory = globalMemory.getTotal();
        long availableMemory = globalMemory.getAvailable();
        Double usedMemoryPercent = (double) (totalMemory-availableMemory)/totalMemory * 100;
        memoryInfo.put("totalMemory", totalMemory);
        memoryInfo.put("availableMemory", availableMemory);
        memoryInfo.put("usedMemoryPercent", usedMemoryPercent);
        // 磁盘
        JSONArray disksArray = new JSONArray();
        List<OSFileStore> fileStores = operatingSystem.getFileSystem().getFileStores();
        for (OSFileStore fs : fileStores) {
            JSONObject diskInfo = new JSONObject();
            diskInfo.put("name", fs.getName());
            diskInfo.put("type", fs.getType());
            diskInfo.put("totalSpaceGB", fs.getTotalSpace() / 1e9);
            diskInfo.put("usableSpaceGB", fs.getUsableSpace() / 1e9);
            diskInfo.put("usagePercent", (1 - (double) fs.getUsableSpace() / fs.getTotalSpace()) * 100);
            disksArray.add(diskInfo);
        }
        // 网络
        JSONArray networksArray = new JSONArray();
        List<NetworkIF> netIfList = hardware.getNetworkIFs();
        for (NetworkIF netIf : netIfList) {
            // 计算网络流量（Bytes/s）
            long rxBytes = netIf.getBytesRecv();
            long txBytes = netIf.getBytesSent();
            JSONObject networkInfo = new JSONObject();
            networkInfo.put("name", netIf.getName());
            networkInfo.put("displayName", netIf.getDisplayName());
            networkInfo.put("ipAddress", netIf.getIPv4addr() != null && netIf.getIPv4addr().length > 0 ? netIf.getIPv4addr()[0] : "N/A");
            networkInfo.put("macAddress", netIf.getMacaddr());
            networkInfo.put("speedMbps", netIf.getSpeed());
            networkInfo.put("rxBytes", rxBytes);
            networkInfo.put("txBytes", txBytes);
            networkInfo.put("rxKbps", rxBytes * 8 / 1024.0); // Kbps
            networkInfo.put("txKbps", txBytes * 8 / 1024.0); // Kbps
            networksArray.add(networkInfo);
        }
        SystemInfoReportVO systemInfoReportVO = SystemInfoReportVO.builder()
                .cpuInfo(cpuInfo)
                .memoryInfo(memoryInfo)
                .diskInfo(disksArray)
                .networkInfo(networksArray)
                .build();
        return CommonResultVO.builder()
                .status(1)
                .message("服务器信息获取成功")
                .data(systemInfoReportVO)
                .build();
    }
}
