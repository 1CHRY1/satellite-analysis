package nnu.mnr.satellite.service.resources;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.vo.resources.GridBoundaryVO;
import nnu.mnr.satellite.model.po.resources.Region;
import nnu.mnr.satellite.model.vo.resources.RegionInfoVO;
import nnu.mnr.satellite.model.vo.resources.ViewWindowVO;
import nnu.mnr.satellite.mapper.resources.IRegionRepo;
import nnu.mnr.satellite.utils.geom.GeometryUtil;
import nnu.mnr.satellite.utils.geom.TileCalculateUtil;
import org.locationtech.jts.geom.Geometry;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/8 20:28
 * @Description:
 */

@Service("RegionDataService")
public class RegionDataService {

    @Autowired
    private ModelMapper regionModelMapper;

    private final IRegionRepo regionRepo;

    public RegionDataService(IRegionRepo regionRepo) {
        this.regionRepo = regionRepo;
    }

    public List<RegionInfoVO> getRegionsByLevel(String level) {
        if (!level.equals("country") && !level.equals("province") && !level.equals("city") && !level.equals("district")) {
            return null;
        }
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("region_level", level);
        List<Region> regions = regionRepo.selectList(queryWrapper);
        return regionModelMapper.map(regions, new TypeToken<List<RegionInfoVO>>() {}.getType());
    }

    public List<RegionInfoVO> getRegionsByParentAndType(Integer parent) {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent", parent);
        List<Region> regions = regionRepo.selectList(queryWrapper);
        return regionModelMapper.map(regions, new TypeToken<List<RegionInfoVO>>() {}.getType());
    }

    public JSONObject getRegionBoundaryById(Integer regionId) throws IOException {
        Region region = getRegionById(regionId);
        return GeometryUtil.geometry2Geojson(region.getBoundary());
    }

    public Region getRegionById(Integer regionId) {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("adcode", regionId);
        return regionRepo.selectOne(queryWrapper);
    }

    public ViewWindowVO getRegionWindowById(Integer regionId) {
        Region region = getRegionById(regionId);
        return ViewWindowVO.builder()
                .center(region.getCenter()).bounds(GeometryUtil.getGeometryBounds(region.getBoundary())).build();
    }

    public List<GridBoundaryVO> getGridsByRegionAndResolution(Integer regionId, Integer resolution) throws IOException {
        Region region = getRegionById(regionId);
        return TileCalculateUtil.getStrictlyCoveredRowColByRegionAndResolution(region.getBoundary(), resolution);
    }

    public String getAddressById(Integer regionId) {
        Region region = getRegionById(regionId);
        List<Integer> acroutes = region.getAcroutes();
        StringBuilder fullName = new StringBuilder();
        for (int i = 0; i < acroutes.size(); i++) {
            // 跳过 i=0; 中国(adcode: 100000)
            if (i == 0)
                continue;
            Region subRegion = getRegionById(acroutes.get(i));
            fullName.append(subRegion.getRegionName());
        }
        fullName.append(region.getRegionName());
        return fullName.toString();
    }

}
