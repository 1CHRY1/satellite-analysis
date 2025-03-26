package nnu.mnr.satellite.service.modeling;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellite.model.dto.common.FileData;
import nnu.mnr.satellite.model.dto.modeling.ProjectBasicDTO;
import nnu.mnr.satellite.model.dto.modeling.ProjectResultDTO;
import nnu.mnr.satellite.model.po.modeling.ProjectResult;
import nnu.mnr.satellite.model.vo.modeling.ProjectResultVO;
import nnu.mnr.satellite.model.vo.resources.SensorInfoVO;
import nnu.mnr.satellite.repository.modeling.IProjectResultRepo;
import nnu.mnr.satellite.utils.data.MinioUtil;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    IProjectResultRepo iProjectResultRepo;

    @Autowired
    ModelMapper projectResultMapper;

    @Autowired
    MinioUtil minioUtil;

    public FileData getProjectResult(ProjectResultDTO projectResultDTO) {
        QueryWrapper<ProjectResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", projectResultDTO.getUserId()).eq("project_id", projectResultDTO.getProjectId()).eq("data_name", projectResultDTO.getName());
        ProjectResult projectResult = iProjectResultRepo.selectOne(queryWrapper);
        String bucket = projectResult.getBucket(); String path = projectResult.getPath();
        return FileData.builder().type(projectResult.getDataType()).stream(minioUtil.downloadByte(bucket, path)).build();
    }

    public List<ProjectResultVO> getProjectResults(ProjectBasicDTO projectBasicDTO) {
        QueryWrapper<ProjectResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", projectBasicDTO.getUserId()).eq("project_id", projectBasicDTO.getProjectId());
        List<ProjectResult> projectResults = iProjectResultRepo.selectList(queryWrapper);
        return projectResultMapper.map(projectResults, new TypeToken<List<ProjectResultVO>>() {}.getType());
    }

}
