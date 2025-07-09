package nnu.mnr.satellite.mapper.modeling;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.modeling.Project;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 22:38
 * @Description:
 */
public interface IProjectRepo extends BaseMapper<Project> {
    @Select("SELECT COUNT(1) > 0 FROM project WHERE project_id = #{projectId}")
    boolean existsById(@Param("projectId") String projectId);
}
