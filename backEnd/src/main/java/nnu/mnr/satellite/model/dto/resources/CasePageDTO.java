package nnu.mnr.satellite.model.dto.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CasePageDTO extends PageDTO {

    private Integer page = 1; //当前页数
    private Integer pageSize = 10; //每页数量
    private Boolean asc = false; //是否顺序，从小到大
    private String searchText = ""; //查询内容
    private String sortField = "createTime"; //排序字段
    private Integer regionId = null;
}
