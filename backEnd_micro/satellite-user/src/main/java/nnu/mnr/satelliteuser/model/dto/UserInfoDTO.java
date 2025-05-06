package nnu.mnr.satelliteuser.model.dto;

import lombok.Data;

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
    private String phone;
    private String province;
    private String city;
    private String title;
    private String organization;
    private String introduction;

}
