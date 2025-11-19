package nnu.mnr.satellite.mapper.modeling;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.modeling.MethlibCase;

@DS("mysql-ard-dev")
public interface IMethlibCaseRepo extends BaseMapper<MethlibCase> {
}
