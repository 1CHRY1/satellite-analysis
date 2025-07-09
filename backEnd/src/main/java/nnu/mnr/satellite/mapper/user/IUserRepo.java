package nnu.mnr.satellite.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.user.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 20:26
 * @Description:
 */

public interface IUserRepo extends BaseMapper<User> {
    @Select("SELECT COUNT(1) > 0 FROM user WHERE user_id = #{userId}")
    boolean existsById(@Param("userId") String userId);
}
