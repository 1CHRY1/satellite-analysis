package nnu.mnr.satellite.model.dto.admin.image;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImagePathsDTO {
    private String bucket;      // 对应image_table中的bucket
    private String tifPath;     // 对应image_table中的tif_path
}
