package nnu.mnr.satellite.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.resources.IProductRepo;
import nnu.mnr.satellite.mapper.resources.ISceneRepo;
import nnu.mnr.satellite.mapper.resources.ISensorRepo;
import nnu.mnr.satellite.model.dto.admin.sensor.SensorDeleteDTO;
import nnu.mnr.satellite.model.dto.admin.sensor.SensorInsertDTO;
import nnu.mnr.satellite.model.dto.admin.sensor.SensorPageDTO;
import nnu.mnr.satellite.model.dto.admin.sensor.SensorUpdateDTO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.model.po.resources.Sensor;
import nnu.mnr.satellite.model.vo.admin.SensorInfoVO;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminSensorService {

    @Autowired
    private ISensorRepo sensorRepo;

    @Autowired
    private ISceneRepo sceneRepo;

    @Autowired
    private IProductRepo productRepo;

    public CommonResultVO getSensorInfoPage(SensorPageDTO sensorPageDTO) {
        // 构造分页对象
        Page<Sensor> page = new Page<>(sensorPageDTO.getPage(), sensorPageDTO.getPageSize());
        LambdaQueryWrapper<Sensor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 筛选
        List<String> dataTypes = sensorPageDTO.getDataTypes();
        if(dataTypes!=null && !dataTypes.isEmpty()) {
            lambdaQueryWrapper.in(Sensor::getDataType, dataTypes);
        }
        String searchText = sensorPageDTO.getSearchText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String trimmedSearchText = searchText.trim();
            lambdaQueryWrapper.and(wrapper ->
                    wrapper.like(Sensor::getSensorName, trimmedSearchText)
                            .or().like(Sensor::getPlatformName, trimmedSearchText)
            );
        }
        // 排序
        String sortField = sensorPageDTO.getSortField();
        Boolean asc = sensorPageDTO.getAsc();
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "sensorName":
                    lambdaQueryWrapper.orderBy(true, asc, Sensor::getSensorName);
                case "sensorId":
                    lambdaQueryWrapper.orderBy(true, asc, Sensor::getSensorId);
                case "platformName":
                    lambdaQueryWrapper.orderBy(true, asc, Sensor::getPlatformName);
                case "dataType":
                    lambdaQueryWrapper.orderBy(true, asc, Sensor::getDataType);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        // 查询，lambdaQueryWrapper没有显式指定selecet，默认select *
        IPage<Sensor> sensorPage = sensorRepo.selectPage(page, lambdaQueryWrapper);
        List<Sensor> sensors = sensorPage.getRecords();
        List<String> sensorIds = sensors.stream().map(Sensor::getSensorId).toList();

        QueryWrapper<Scene> sceneWrapper = new QueryWrapper<>();
        sceneWrapper.in("sensor_id", sensorIds)
                .groupBy("sensor_id")
                .select("sensor_id", "COUNT(*) as sceneCount");

        // 执行查询并转换为Map<sensorId, count>
        List<Map<String, Object>> sceneCounts = sceneRepo.selectMaps(sceneWrapper);
        Map<String, Integer> countMap = sceneCounts.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("sensor_id"),
                        m -> ((Number) m.get("sceneCount")).intValue()
                ));

        // 4. 将景的数量映射回传感器对象
        List<SensorInfoVO> sensorInfoVOs = sensors.stream().map(sensor -> {
            SensorInfoVO sensorInfoVO = new SensorInfoVO();
            // 复制传感器属性
            BeanUtils.copyProperties(sensor, sensorInfoVO);
            // 设置景的数量
            sensorInfoVO.setSceneCount(countMap.getOrDefault(sensor.getSensorId(), 0));
            return sensorInfoVO;
        }).toList();

        // 5. 构建返回结果（需要自定义一个包含count的分页对象或单独返回count）
        // 这里我们创建一个新的分页对象来包含转换后的数据
        IPage<SensorInfoVO> sensorInfoVOPage = new Page<>();
        sensorInfoVOPage.setCurrent(sensorPage.getCurrent());
        sensorInfoVOPage.setSize(sensorPage.getSize());
        sensorInfoVOPage.setTotal(sensorPage.getTotal());
        sensorInfoVOPage.setRecords(sensorInfoVOs);

        return CommonResultVO.builder()
                .status(1)
                .message("传感器信息获取成功")
                .data(sensorInfoVOPage)
                .build();
    }

    public CommonResultVO insertSensor(SensorInsertDTO sensorInsertDTO){
        String sensorName = sensorInsertDTO.getSensorName();
        String platformName = sensorInsertDTO.getPlatformName();
        if(sensorName == null || platformName == null){
            return CommonResultVO.builder()
                    .status(-1)
                    .message("新增失败，传感器名称或者平台名称为空")
                    .build();
        }
        if(sensorRepo.existsBySensorName(sensorName)){
            Sensor sensor = sensorRepo.selectBySensorName(sensorName);
            return CommonResultVO.builder()
                    .status(-1)
                    .message("新增失败，传感器名称已存在")
                    .data(sensor)
                    .build();
        }
        String sensorId = IdUtil.generateSensorId();
        Sensor sensor = new Sensor();
        sensor.setSensorId(sensorId);
        sensor.setSensorName(sensorName);
        sensor.setPlatformName(platformName);
        sensor.setDescription(sensorInsertDTO.getDescription());
        sensor.setDataType(sensorInsertDTO.getDataType());
        sensorRepo.insert(sensor);
        return CommonResultVO.builder()
                .status(1)
                .message("新增传感器成功")
                .build();
    }

    public CommonResultVO updateSensor(SensorUpdateDTO sensorUpdateDTO){
        if (sensorUpdateDTO.getSensorId() == null){
            return CommonResultVO.builder()
                    .status(-1)
                    .message("更新传感器信息失败，sensorId不能为空")
                    .build();
        }
        Sensor sensor = new Sensor();
        BeanUtils.copyProperties(sensorUpdateDTO, sensor);
        sensorRepo.updateById(sensor);
        return CommonResultVO.builder()
                .status(1)
                .message("更新传感器信息成功")
                .build();
    }

    public CommonResultVO deleteSensor(SensorDeleteDTO sensorDeleteDTO){
        // 1. 检查是否有景关联这些传感器
        List<String> sensorIds = sensorDeleteDTO.getSensorIds();
        boolean hasScenes = sceneRepo.existsBySensorIds(sensorIds);
        boolean hasProducts = productRepo.existsBySensorIds(sensorIds);
        if (hasScenes || hasProducts) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("删除失败：仍有景或产品属于该传感器，请先解除关联")
                    .build();
        }

        // 2. 如果没有关联用户，执行删除
        sensorRepo.deleteByIds(sensorIds);
        return CommonResultVO.builder()
                .status(1)
                .message("删除传感器成功")
                .build();
    }




}
