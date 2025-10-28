package nnu.mnr.satellite.model.dto.admin.image;

import lombok.Data;

@Data
public class ImageUpdateDTO {
    private String imageId;
    private String band;
    private float cloud;
}
