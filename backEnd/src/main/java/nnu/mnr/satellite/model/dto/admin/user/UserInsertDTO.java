package nnu.mnr.satellite.model.dto.admin.user;

import lombok.Data;

@Data
public class UserInsertDTO {
    private String userName;
    private String email;
    private String password;;
    private String title;
    private String organization;
    private Integer roleId;
}
