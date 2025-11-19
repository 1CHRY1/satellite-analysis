package nnu.mnr.satellite.mapper.modeling;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.modeling.Tag;
import org.springframework.stereotype.Repository;

@Repository
@DS("mysql-ard-dev")
public interface ITagRepo extends BaseMapper<Tag> {
}
