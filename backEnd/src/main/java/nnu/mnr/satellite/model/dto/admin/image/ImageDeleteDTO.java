package nnu.mnr.satellite.model.dto.admin.image;

import lombok.Data;

import java.util.List;

@Data
public class ImageDeleteDTO {
    private List<String> imageIds;
}
