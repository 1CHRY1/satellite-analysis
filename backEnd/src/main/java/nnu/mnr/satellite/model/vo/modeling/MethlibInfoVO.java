package nnu.mnr.satellite.model.vo.modeling;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.JSONArrayTypeHandler;
import nnu.mnr.satellite.utils.typeHandlerEx.TagIdsTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MethlibInfoVO {
    private Integer id;
    private String name;
    private String description;
    private String longDesc;
    private String nameZh;
    private String descriptionZh;
    private String longDescZh;
    private String copyright;
    private String category;
    private String uuid;
    private String type;
    @TableField(typeHandler = JSONArrayTypeHandler.class)
    private JSONArray params;
    @TableField(typeHandler = JSONArrayTypeHandler.class)
    private JSONArray paramsZh;
    private String execution;
    private LocalDateTime createTime;
    @TableField(typeHandler = TagIdsTypeHandler.class)
    private List<Integer> tagIds;
}
