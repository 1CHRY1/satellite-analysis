package nnu.mnr.satellite.model.dto.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/29 15:37
 * @Description:
 */

@Data
public class UserInfoDTO {

    private String userId;
    private String userName;
    private String phone;
    private String province;
    private String city;
    private String email;
    private String title;
    private String organization;
    private String introduction;

}
