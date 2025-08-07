package nnu.mnr.satellite.service.resources;

import com.baomidou.dynamic.datasource.annotation.DS;
import nnu.mnr.satellite.cache.SceneDataCache;
import nnu.mnr.satellite.mapper.resources.IVectorRepo;
import nnu.mnr.satellite.model.dto.resources.VectorsFetchDTO;
import nnu.mnr.satellite.model.dto.resources.VectorsLocationFetchDTO;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.vo.resources.VectorInfoVO;
import nnu.mnr.satellite.model.vo.resources.VectorTypeVO;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

import static nnu.mnr.satellite.utils.geom.TileCalculateUtil.getTileGeomByIdsAndResolution;


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

    public byte[] getVectorByRegionAndTableName(String tableName, int z, int x, int y, String cacheKey, Integer type){
        SceneDataCache.UserRegionInfoCache userRegionInfoCache = SceneDataCache.getUserRegionInfoCacheMap(cacheKey);
        Geometry gridBoundary = userRegionInfoCache.gridsBoundary;
        String wkt = gridBoundary.toText();
        return getMvtTile(tableName, wkt, z, x, y, type);
    }

    public byte[] getVectorByLocationAndTableName(String locationId, Integer resolution, String tableName, int z, int x, int y, Integer type){
        String wkt = locationService.getLocationBoundary(resolution, locationId).toText();
        return getMvtTile(tableName, wkt, z, x, y, type);
    }

    public byte[] getVectorByGridResolutionAndTableName(Integer columnId, Integer rowId, Integer resolution, String tableName, int z, int x, int y, Integer type){
        String wkt = getTileGeomByIdsAndResolution(rowId,  columnId, resolution).toString();
        return getMvtTile(tableName, wkt, z, x, y, type);
    }

    // 获取矢量数据并发布成瓦片服务
    @DS("pg_satellite")
    public byte[] getMvtTile(String tableName, String wkt, int z, int x, int y, Integer type){
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
        Object mvtResult = vectorRepo.getVectorByTableNameAndGeometry(tableName, wkt, z, x, y, columns, type);
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
