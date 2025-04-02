package nnu.mnr.satellite.model.dto.user;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 23:06
 * @Description:
 */

@Data
public class UserLoginDTO {

    String email;
    String userName;
    String password;

}
