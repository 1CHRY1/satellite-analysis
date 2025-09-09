package nnu.mnr.satellite.model.dto.minIO;

import lombok.Data;

@Data
public class FileEditDTO {
    private String oldFilePath;
    private String newFilePath;
}
