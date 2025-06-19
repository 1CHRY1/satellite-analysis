package nnu.mnr.satellite.service.modeling;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.dto.common.FileData;
import nnu.mnr.satellite.model.dto.modeling.ProjectBasicDTO;
import nnu.mnr.satellite.model.dto.modeling.ProjectResultDTO;
import nnu.mnr.satellite.model.po.modeling.ProjectResult;
import nnu.mnr.satellite.model.pojo.modeling.MinioProperties;
import nnu.mnr.satellite.model.pojo.modeling.TilerProperties;
import nnu.mnr.satellite.model.vo.modeling.JsonResultVO;
import nnu.mnr.satellite.model.vo.modeling.ProjectResultVO;
import nnu.mnr.satellite.model.vo.modeling.TilerResultVO;
import nnu.mnr.satellite.mapper.modeling.IProjectResultRepo;
import nnu.mnr.satellite.utils.dt.MinioUtil;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/25 10:22
 * @Description:
 */

@Service
public class ProjectResultDataService {

    @Autowired
    ModelMapper projectResultMapper;

    @Autowired
    MinioUtil minioUtil;

    @Autowired
    TilerProperties tilerProperties;

    private final IProjectResultRepo projectResultRepo;
    @Autowired
    private MinioProperties minioProperties;

    public ProjectResultDataService(IProjectResultRepo iProjectResultRepo) {
        this.projectResultRepo = iProjectResultRepo;
    }

    public FileData getProjectResult(ProjectResultDTO projectResultDTO) {
        QueryWrapper<ProjectResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", projectResultDTO.getUserId()).eq("project_id", projectResultDTO.getProjectId()).eq("data_name", projectResultDTO.getName());
        ProjectResult projectResult = projectResultRepo.selectOne(queryWrapper);
        String bucket = projectResult.getBucket(); String path = projectResult.getPath();
        return FileData.builder().type(projectResult.getDataType()).stream(minioUtil.downloadByte(bucket, path)).build();
    }

    public TilerResultVO getProjectTifResult(String dataId) {
        QueryWrapper<ProjectResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("data_id", dataId);
        ProjectResult projectResult = projectResultRepo.selectOne(queryWrapper);
        if (!Objects.equals(projectResult.getDataType(), "tiff")
                && !Objects.equals(projectResult.getDataType(), "tif")
                && !Objects.equals(projectResult.getDataType(), "TIF") ) {
            return TilerResultVO.tilerBuilder().build();
        }
        return TilerResultVO.tilerBuilder()
                .tilerUrl(tilerProperties.getEndPoint())
                .object(projectResult.getBucket() + projectResult.getPath())
                .minioEndpoint(minioProperties.getUrl())
                .build();
    }

    public JsonResultVO getProjectJsonResult(String dataId) {
        QueryWrapper<ProjectResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("data_id", dataId);
        ProjectResult projectResult = projectResultRepo.selectOne(queryWrapper);
        if (!Objects.equals(projectResult.getDataType(), "json")) {
            return JsonResultVO.jsonResultBuilder().build();
        }
        String jsonString = minioUtil.readJsonFile(projectResult.getBucket(), projectResult.getPath());
        return JsonResultVO.jsonResultBuilder()
                .type(projectResult.getDataType())
                .data(JSONObject.parseObject(jsonString))
                .build();
    }

    public List<ProjectResultVO> getProjectResults(ProjectBasicDTO projectBasicDTO) {
        QueryWrapper<ProjectResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", projectBasicDTO.getUserId()).eq("project_id", projectBasicDTO.getProjectId());
        List<ProjectResult> projectResults = projectResultRepo.selectList(queryWrapper);
        return projectResultMapper.map(projectResults, new TypeToken<List<ProjectResultVO>>() {}.getType());
    }

}
