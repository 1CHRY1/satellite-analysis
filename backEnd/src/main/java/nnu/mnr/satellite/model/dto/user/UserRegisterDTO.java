package nnu.mnr.satellite.model.dto.user;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/27 20:23
 * @Description:
 */

@Data
public class UserRegisterDTO {

    private String userName;
    private String email;
    private String password;;
    private String title;
    private String organization;

}
