package nnu.mnr.satelliteopengmp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Description
 * @Author wyjq
 * @Date 2022/3/17
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO implements Serializable {

    private Integer page = 1; //当前页数
    private Integer pageSize = 10; //每页数量
    private Boolean asc = false; //是否顺序，从小到大
    private String searchText = ""; //查询内容
    private String sortField = "createTime"; //排序字段

}

