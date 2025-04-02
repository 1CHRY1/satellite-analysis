package nnu.mnr.satellite.opengmp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description
 * @Author wyjq
 * @Date 2022/3/17
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourcePageDTO extends PageDTO {

    private Integer page = 1; //当前页数
    private Integer pageSize = 10; //每页数量
    private Boolean asc = false; //是否顺序，从小到大
    private String searchText = ""; //查询内容
    private String sortField = "createTime"; //排序字段
    private String tagClass = "problemTags"; //标签的类型
    private String tagName = "全球变化与区域环境演化"; //标签的名字,用于单标签资源查询
    private List<String> tagNames = new ArrayList<>(Arrays.asList("tag1", "tag2", "tag3"));; //标签的名字,用于多标签模型/方法查询

}

