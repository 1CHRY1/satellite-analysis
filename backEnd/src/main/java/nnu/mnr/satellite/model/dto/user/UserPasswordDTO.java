package nnu.mnr.satellite.model.dto.user;

import lombok.Data;

@Data
public class UserPasswordDTO {
    String userId;
    String userName;
    String oldPassword;
    String newPassword;
}
