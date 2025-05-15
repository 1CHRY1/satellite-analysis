package nnu.mnr.satellite.service.resources;

import nnu.mnr.satellite.model.dto.modeling.ModelServerImageDTO;
import nnu.mnr.satellite.model.dto.modeling.ModelServerSceneDTO;
import nnu.mnr.satellite.model.dto.resources.GridBasicDTO;
import nnu.mnr.satellite.model.vo.resources.GridSceneVO;
import nnu.mnr.satellite.model.dto.resources.GridSceneFetchDTO;
import nnu.mnr.satellite.model.po.resources.Scene;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // 获取每个网格中的影像
    public List<GridSceneVO> getScenesFromGrids(GridSceneFetchDTO gridSceneFetchDTO) {
        List<GridSceneVO> gridRess = new ArrayList<>();
        List<GridBasicDTO> grids = gridSceneFetchDTO.getGrids();
        for (GridBasicDTO grid : grids) {
            List<String> sceneIds = gridSceneFetchDTO.getSceneIds();
            List<ModelServerSceneDTO> sceneDtos = new ArrayList<>();
            for (String sceneId : sceneIds) {
                Scene scene = sceneDataService.getSceneById(sceneId);
                Geometry gridPoly = TileCalculateUtil.getTileGeomByIdsAndResolution(grid.getRowId(), grid.getColumnId(), grid.getResolution());
                if (scene.getBbox().disjoint(gridPoly) ) {
                    continue;
                }
                List<ModelServerImageDTO> imageDTOS = imageDataService.getModelServerImageDTOBySceneId(sceneId);
                ModelServerSceneDTO sceneDto = ModelServerSceneDTO.builder()
                        .sceneId(sceneId).cloudPath(scene.getCloudPath())
                        .sceneTime(scene.getSceneTime()).bucket(scene.getBucket()).images(imageDTOS).build();
                sceneDtos.add(sceneDto);
            }
            GridSceneVO gridRes = GridSceneVO.builder()
                    .scenes(sceneDtos).rowId(grid.getRowId()).columnId(grid.getColumnId())
                    .resolution(grid.getResolution()).build();
            gridRess.add(gridRes);
        }
        return gridRess;
    }

}
