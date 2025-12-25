package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.mapper.resources.IVectorRepo;
import nnu.mnr.satellite.model.dto.resources.MinMaxResult;
import nnu.mnr.satellite.model.dto.resources.VectorsFetchDTO;
import nnu.mnr.satellite.model.dto.resources.VectorsLocationFetchDTO;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.vo.resources.VectorInfoVO;
import nnu.mnr.satellite.model.vo.resources.VectorTypeVO;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static nnu.mnr.satellite.utils.geom.TileCalculateUtil.getTileGeomByIdsAndResolution;

@DS("pg-satellite")
@Service("VectorDataService")
public class VectorDataService {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    @Autowired
    private RegionDataService regionDataService;
    @Autowired
    IVectorRepo vectorRepo;
    @Autowired
    LocationService locationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<VectorInfoVO> getVectorByTimeAndRegion(VectorsFetchDTO vectorsFetchDTO) {
        String startTime = vectorsFetchDTO.getStartTime();
        String endTime = vectorsFetchDTO.getEndTime();
        Integer regionId = vectorsFetchDTO.getRegionId();
        Region region = regionDataService.getRegionById(regionId);
        String wkt = region.getBoundary().toText();
        return vectorRepo.getVectorsDesByTimeAndGeometry(startTime, endTime, wkt);
    }

    public List<VectorInfoVO>  getVectorByTimeAndLocation(VectorsLocationFetchDTO vectorsLocationFetchDTO){
        String startTime = vectorsLocationFetchDTO.getStartTime();
        String endTime = vectorsLocationFetchDTO.getEndTime();
        String locationId = vectorsLocationFetchDTO.getLocationId();
        Integer resolution = vectorsLocationFetchDTO.getResolution();
        Geometry boundary = locationService.getLocationBoundary(resolution, locationId);
        String wkt = boundary.toText();
        return vectorRepo.getVectorsDesByTimeAndGeometry(startTime, endTime, wkt);
    }

    public List<VectorTypeVO> getVectorTypeByTableName(String tableName){
        return vectorRepo.getVectorTypeByTableName(tableName);
    }

    public List<String> getVectorTypeByTableNameAndField(String tableName, String field, String cacheKey){
        SceneDataCache.UserRegionInfoCache userRegionInfoCache = SceneDataCache.getUserRegionInfoCacheMap(cacheKey);
        Geometry gridBoundary = userRegionInfoCache.gridsBoundary;
        String wkt = gridBoundary.toText();
        return vectorRepo.getVectorTypeByTableNameAndField(tableName, field, wkt);
    }

    public List<JSONObject> getVectorTypeByTableNameAndFieldAndCount(String tableName, String field, Integer count, String cacheKey){
        SceneDataCache.UserRegionInfoCache userRegionInfoCache = SceneDataCache.getUserRegionInfoCacheMap(cacheKey);
        Geometry gridBoundary = userRegionInfoCache.gridsBoundary;
        String wkt = gridBoundary.toText();
        MinMaxResult MinMaxList = vectorRepo.getMinMaxByTableNameAndFieldAndCount(tableName, field, wkt);
        if (MinMaxList == null) {
            return Collections.emptyList();
        }
        float min = MinMaxList.getMin();
        float max = MinMaxList.getMax();
        // 3. 处理特殊情况
        if (count == null || count <= 0) count = 1;
        if (Math.abs(max - min) < 1e-10) {
            JSONObject segment = new JSONObject();
            segment.put("up", max);
            segment.put("low", min);
            return Collections.singletonList(segment);
        }

        // 4. 计算分段
        double range = max - min;
        double segmentSize = range / count;
        List<JSONObject> segments = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double low = max - (i + 1) * segmentSize;
            double up = max - i * segmentSize;
            if (i == count - 1) low = min;

            JSONObject segment = new JSONObject();
            segment.put("up", up);
            segment.put("low", low);
            segments.add(segment);
        }
        return segments;
    }

    public byte[] getVectorByRegionAndTableName(String tableName, String field, int z, int x, int y, String cacheKey, List<String> types){
        SceneDataCache.UserRegionInfoCache userRegionInfoCache = SceneDataCache.getUserRegionInfoCacheMap(cacheKey);
        Geometry gridBoundary = userRegionInfoCache.gridsBoundary;
        String wkt = gridBoundary.toText();
        return getMvtTile(tableName, wkt, field, z, x, y, types);
    }

    public byte[] getVectorByLocationAndTableName(String locationId, Integer resolution, String tableName, String field, int z, int x, int y, List<String> types){
        String wkt = locationService.getLocationBoundary(resolution, locationId).toText();
        return getMvtTile(tableName, wkt, field, z, x, y, types);
    }

    public byte[] getVectorByGridResolutionAndTableName(Integer columnId, Integer rowId, Integer resolution, String tableName, String field, int z, int x, int y, List<String> types){
        String wkt = getTileGeomByIdsAndResolution(rowId,  columnId, resolution).toString();
        return getMvtTile(tableName, wkt, field, z, x, y, types);
    }

    // 获取矢量数据并发布成瓦片服务
    @Transactional
    public byte[] getMvtTile(String tableName, String wkt, String field, int z, int x, int y, List<String> types){
        // 参数校验，防止sql注入
        validateParams(tableName, z, x, y);
        // 查询表的非几何字段列表（排除 'geom' 字段）
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = 'gis_db' AND table_name = ? AND column_name != 'geom'",
                String.class,
                tableName
        );
        // 调用Mapper查询MVT数据
        Object mvtResult = vectorRepo.getVectorByTableNameAndGeometry(tableName, wkt, field, z, x, y, columns, types);
        // 类型转换与返回
        if (mvtResult instanceof byte[]) {
            return (byte[]) mvtResult;
        } else {
            throw new IllegalStateException("Unexpected MVT result type: " +
                    (mvtResult != null ? mvtResult.getClass() : "null"));
        }
    }

    /**
     * 校验表名和瓦片坐标参数
     */
    private void validateParams(String tableName, int z, int x, int y) {
        // 表名校验（防止SQL注入）
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        // 瓦片坐标范围校验（根据实际需求调整）
        if (z < 0 || z > 30) {
            throw new IllegalArgumentException("Zoom level must be between 0 and 30");
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Tile coordinates must be positive");
        }
    }
}
