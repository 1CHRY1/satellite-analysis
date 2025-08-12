package nnu.mnr.satellite.mapper.user;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.user.Record;

@DS("mysql_ard_iam")
public interface IRecordRepo extends BaseMapper<Record> {
}
