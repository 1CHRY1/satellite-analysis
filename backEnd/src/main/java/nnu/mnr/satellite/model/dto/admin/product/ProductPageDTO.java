package nnu.mnr.satellite.model.dto.admin.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class ProductPageDTO extends PageDTO {
    private Boolean asc = true; //是否顺序，从小到大
    private String sortField = "resolution"; //排序字段
    private List<String> sensorIds;
}
