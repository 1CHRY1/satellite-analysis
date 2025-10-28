package nnu.mnr.satellite.model.dto.admin.user;

import lombok.Data;

import java.util.List;

@Data
public class UserDeleteDTO {
    private List<String> userIds;
}
