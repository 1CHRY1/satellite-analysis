package nnu.mnr.satellite.service.resources;

import nnu.mnr.satellite.mapper.resources.IVectorRepo;
import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.dto.resources.GridBasicDTO;
import nnu.mnr.satellite.model.dto.resources.GridVectorFetchDTO;
import nnu.mnr.satellite.model.po.geo.GeoLocation;
import nnu.mnr.satellite.model.po.resources.SceneSP;
import nnu.mnr.satellite.model.po.resources.Vector;
import nnu.mnr.satellite.model.vo.resources.GridBoundaryVO;
import nnu.mnr.satellite.model.vo.resources.GridSceneVO;
import nnu.mnr.satellite.model.dto.resources.GridSceneFetchDTO;
import nnu.mnr.satellite.model.vo.resources.VectorInfoVO;
import nnu.mnr.satellite.service.common.BandMapperGenerator;
import nnu.mnr.satellite.utils.common.ConcurrentUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import nnu.mnr.satellite.model.vo.resources.GridVectorVO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/13 13:56
 * @Description:
 */

@Service("GridDataService")
public class GridDataService {

    @Autowired
    private SceneDataServiceV2 sceneDataService;

    @Autowired
    private ImageDataService imageDataService;

    @Autowired
    private BandMapperGenerator bandMapperGenerator;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IVectorRepo vectorRepo;

    public List<GridBoundaryVO> getGridsByLocationId(String locationId, Integer resolution) throws IOException {
        List<GridBoundaryVO> grids = new ArrayList<>();
        GeoLocation location = locationService.searchById(locationId);
        double lon = Double.parseDouble(location.getWgs84Lon());
        double lat = Double.parseDouble(location.getGcj02Lat());
        int[] grid = TileCalculateUtil.getGridXYByLngLatAndResolution(lon, lat, resolution);
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1]-1, grid[0]-1, resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1]-1, grid[0], resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1]-1, grid[0]+1, resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1], grid[0]-1, resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1], grid[0], resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1], grid[0]+1, resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1]+1, grid[0]-1, resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1]+1, grid[0], resolution));
        grids.add(TileCalculateUtil.getTileBoundaryByIdsAndResolution(grid[1]+1, grid[0]+1, resolution));
        return grids;
    }

    // 获取每个网格中的影像
    public List<GridSceneVO> getScenesFromGrids(GridSceneFetchDTO gridSceneFetchDTO) {
        List<GridBasicDTO> grids = gridSceneFetchDTO.getGrids();
        List<String> sceneIds = gridSceneFetchDTO.getSceneIds();
        List<SceneSP> sceneSps = sceneDataService.getScenesByIdsWithProductAndSensor(sceneIds);
        Function<GridBasicDTO, GridSceneVO> mapper = grid -> {
            List<ModelServerSceneDTO> sceneDtos;
            Geometry gridPoly = TileCalculateUtil.getTileGeomByIdsAndResolution(grid.getRowId(), grid.getColumnId(), grid.getResolution());

            // 封装每个 scene 的处理逻辑
            Function<SceneSP, ModelServerSceneDTO> sceneMapper = scene -> {
                if (!scene.getBbox().contains(gridPoly)) {
                    return null; // 不符合条件的 scene 返回 null
                }
                List<ModelServerImageDTO> imageDTOS = imageDataService.getModelServerImageDTOBySceneId(scene.getSceneId());
                return ModelServerSceneDTO.builder()
                        .sceneId(scene.getSceneId())
                        .cloudPath(scene.getCloudPath())
                        .sensorName(scene.getSensorName())
                        .productName(scene.getProductName())
                        .resolution(scene.getResolution())
                        .sceneTime(scene.getSceneTime())
                        .bandMapper(bandMapperGenerator.getSatelliteConfigBySensorName(scene.getSensorName()))
                        .bucket(scene.getBucket())
                        .dataType(scene.getDataType())
                        .noData(scene.getNoData())
                        .images(imageDTOS)
                        .build();
            };

            try {
                // 并发处理每个 scene
                List<ModelServerSceneDTO> results = ConcurrentUtil.processConcurrently(sceneSps, sceneMapper);
                // 过滤掉不符合条件的 null 值
                sceneDtos = results.stream().filter(Objects::nonNull).collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("Failed to process scenes concurrently", e);
            }

            return GridSceneVO.builder()
                    .scenes(sceneDtos)
                    .rowId(grid.getRowId())
                    .columnId(grid.getColumnId())
                    .resolution(grid.getResolution())
                    .build();
        };
        try {
            // 使用 ConcurrentUtil 进行并发处理
            return ConcurrentUtil.processConcurrently(grids, mapper);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process grids concurrently", e);
        }
    }

    public List<GridVectorVO> getVectorsFromGrids(GridVectorFetchDTO gridVectorFetchDTO) {
        List<GridBasicDTO> grids = gridVectorFetchDTO.getGrids();
        List<String> tableNames = gridVectorFetchDTO.getTableNames();
        List<GridVectorVO> result = new ArrayList<>();
        for (GridBasicDTO grid : grids) {
            Geometry gridPoly = TileCalculateUtil.getTileGeomByIdsAndResolution(grid.getRowId(), grid.getColumnId(), grid.getResolution());
            String gridPolyText = gridPoly.toText();
            List<VectorInfoVO> vectorInfoList = vectorRepo.findIntersectingVectors(gridPolyText, tableNames);
            GridVectorVO gridVectorVO = GridVectorVO.builder()
                    .rowId(grid.getRowId())
                    .columnId(grid.getColumnId())
                    .vectors(vectorInfoList)
                    .build();
            result.add(gridVectorVO);
        }
        return result;
    }


}
