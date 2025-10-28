package nnu.mnr.satellite.model.dto.admin.product;


import lombok.Data;

import java.util.List;

@Data
public class ProductDeleteDTO {
    private List<String> productIds;
}
