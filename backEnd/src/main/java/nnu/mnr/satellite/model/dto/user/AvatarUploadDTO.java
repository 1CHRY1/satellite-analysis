package nnu.mnr.satellite.model.dto.user;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AvatarUploadDTO {
    MultipartFile file;
    String userId;
    String userName;
}
