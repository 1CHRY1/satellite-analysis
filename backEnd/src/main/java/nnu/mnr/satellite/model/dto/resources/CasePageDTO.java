package nnu.mnr.satellite.model.dto.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.opengmp.model.dto.PageDTO;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

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
    // 序列化和反序列化
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime = null;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime = null;
    private Integer resolution = null;
}
