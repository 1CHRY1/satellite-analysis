package nnu.mnr.satelliteresource.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satelliteresource.model.po.Region;
import nnu.mnr.satelliteresource.model.vo.resources.RegionInfoVO;
import nnu.mnr.satelliteresource.repository.IRegionRepo;
import nnu.mnr.satelliteresource.utils.geom.GeometryUtil;
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

}
