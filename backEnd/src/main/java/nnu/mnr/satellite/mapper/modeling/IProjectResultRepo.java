package nnu.mnr.satellite.mapper.modeling;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.modeling.ProjectResult;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/25 10:25
 * @Description:
 */
@DS("mysql_ard_dev")
public interface IProjectResultRepo extends BaseMapper<ProjectResult> {
}
