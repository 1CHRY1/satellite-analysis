package nnu.mnr.satellite.model.dto.admin.user;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String userId;
    private String userName;
    private String password;
    private String phone;
    private String province;
    private String city;
    private String email;
    private String title;
    private String organization;
    private String introduction;
    private Integer roleId;
}
