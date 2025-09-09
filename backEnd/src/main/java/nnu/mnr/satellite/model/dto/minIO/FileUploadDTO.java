package nnu.mnr.satellite.model.dto.minIO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadDTO {
    private String filePath;
    private MultipartFile file;
}
