package nnu.mnr.satellite.service.resources;

import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.enums.common.SceneTypeByResolution;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.resources.GridBasicDTO;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.resources.CoverageReportVO;
import nnu.mnr.satellite.model.vo.resources.SceneDesVO;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import com.alibaba.fastjson2.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static nnu.mnr.satellite.utils.geom.TileCalculateUtil.getTileGeomByIdsAndResolution;

@Service("GridDataServiceV3")
public class GridDataServiceV3 {

    @Autowired
    private ImageDataService imageDataService;

    @Autowired
    private BandMapperGenerator bandMapperGenerator;

    @Autowired
    private SceneDataServiceV3 sceneDataService;

    public CoverageReportVO<JSONObject> getScenesByGridAndResolution(GridBasicDTO gridBasicDTO, String cacheKey){
        Integer columnId = gridBasicDTO.getColumnId();
        Integer rowId = gridBasicDTO.getRowId();
        Integer resolution = gridBasicDTO.getResolution();
        Geometry wkt = getTileGeomByIdsAndResolution(rowId, columnId, resolution);
        SceneDataCache.UserSceneCache userSceneCache = SceneDataCache.getUserSceneCacheMap(cacheKey);
        if (userSceneCache == null) {
            throw new NullPointerException("No corresponding cache found, please log in again or retrieve the data");
        }
        List<SceneDesVO> scenesInfo = userSceneCache.scenesInfo;
        if (scenesInfo == null || scenesInfo.isEmpty()) {
            throw new NullPointerException("No scene data available in the cache.");
        }
        // 筛选出与格网相交的景
        List<SceneDesVO> filteredScenesInfo = scenesInfo.stream()
                .filter(scene -> isSceneIntersectGrid(scene, wkt))
                .toList();

        CoverageReportVO<JSONObject> report = new CoverageReportVO<>();
        List<String> category = SceneTypeByResolution.getCategoryNames();
        Integer total = filteredScenesInfo.size();
        report.setTotal(total); // 总数据量
        report.setCategory(category); // 分类名称列表

        Map<String, CoverageReportVO.DatasetItemVO<JSONObject>> dataset = new LinkedHashMap<>();
        for (SceneTypeByResolution type : SceneTypeByResolution.values()) {
            // 初始化每个分类的 DatasetItemVO
            CoverageReportVO.DatasetItemVO<JSONObject> item = new CoverageReportVO.DatasetItemVO<>();
            item.setLabel(type.getLabel());
            item.setResolution((float) type.getResolution());
            // 筛选当前分类的场景数据（根据 resolution 字段匹配）
            List<SceneDesVO> filteredScenes = filteredScenesInfo.stream()
                    .filter(scene -> sceneDataService.isSceneMatchResolutionType(scene, type))
                    .toList();
            item.setTotal(filteredScenes.size());
            // 构造dataList
            List<JSONObject> dataList = new ArrayList<>();
            if (!filteredScenes.isEmpty()) {
                for(SceneDesVO sceneInfo : filteredScenes){
                    JSONObject scene = new JSONObject();
                    scene.put("sceneId", sceneInfo.getSceneId());
                    scene.put("sceneTime", sceneInfo.getSceneTime());
                    scene.put("noData", sceneInfo.getNoData());
                    scene.put("sensorName", sceneInfo.getSensorName());
                    scene.put("productName", sceneInfo.getProductName());
                    List<ModelServerImageDTO> images = imageDataService.getModelServerImageDTOBySceneId(sceneInfo.getSceneId());
                    scene.put("images", images);
                    JSONObject bandMapper = bandMapperGenerator.getSatelliteConfigBySensorName(sceneInfo.getSensorName());
                    scene.put("bandMapper", bandMapper);
                    dataList.add(scene);
                }
            }
            // 排序
            List<JSONObject> sortedDataList = dataList.stream()
                    .sorted((scene1, scene2) -> {
                        // 按 sceneTime 降序排序（最新的在前）
                        LocalDateTime time1 = LocalDateTime.parse(scene1.getString("sceneTime"));
                        LocalDateTime time2 = LocalDateTime.parse(scene2.getString("sceneTime"));
                        return time2.compareTo(time1); // 降序
                    })
                    .toList(); // 重新收集成 List
            item.setDataList(sortedDataList);
            dataset.put(type.name(), item);
        }
        report.setDataset(dataset);
        return report;
    }

    private boolean isSceneIntersectGrid(SceneDesVO scene, Geometry wkt){
        return wkt.intersects(scene.getBoundingBox());
    }

}
