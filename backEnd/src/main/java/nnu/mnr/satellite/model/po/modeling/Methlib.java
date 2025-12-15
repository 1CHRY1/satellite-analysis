package nnu.mnr.satellite.model.po.modeling;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.JSONArrayTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("methlib_table")
public class Methlib {
    @TableId
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
}
