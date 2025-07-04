package nnu.mnr.satellitemodeling.service.modeling;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import nnu.mnr.satellitemodeling.model.po.modeling.Project;
import nnu.mnr.satellitemodeling.model.vo.modeling.ProjectVO;
import nnu.mnr.satellitemodeling.repository.modeling.IProjectRepo;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 22:38
 * @Description:
 */

@Service
public class ProjectDataService {

    @Autowired
    private ModelMapper projectModelMapper;

    private final IProjectRepo projectRepo;

    public ProjectDataService(IProjectRepo projectRepo) {
        this.projectRepo = projectRepo;
    }

    public Project getProjectByUserAndName(String userId, String projectName) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.select().eq("create_user", userId).eq("project_name", projectName);
        return projectRepo.selectOne(queryWrapper);
    }

    public Project getProjectById(String projectId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.select().eq("project_id", projectId);
        return projectRepo.selectOne(queryWrapper);
    }

    public List<ProjectVO> getProjectsByUserId(String userId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_user", userId);
        List<Project> projects = projectRepo.selectList(queryWrapper);
        return projectModelMapper.map(projects, new TypeToken<List<ProjectVO>>() {}.getType());
    }

    public List<ProjectVO> getAllProjects() {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderBy(true, false, "create_time");
        List<Project> projects = projectRepo.selectList(queryWrapper);
        return projectModelMapper.map(projects, new TypeToken<List<ProjectVO>>() {}.getType());
    }

    public boolean VerifyUserProject(String userId, String projectId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper();
        queryWrapper.orderBy(true, false, "create_time");
        queryWrapper.eq("create_user", userId).eq("project_id", projectId);
        Project project = projectRepo.selectOne(queryWrapper);
        if (project == null) {
            return false;
        }
        return project.getJoinedUsers().contains(userId) || project.getCreateUser().equals(userId);
    }

}
