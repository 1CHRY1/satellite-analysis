package nnu.mnr.satellite.model.po.user;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.utils.typeHandler.FastJson2TypeHandler;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("record_table")
public class Record {
    @TableId(type = IdType.AUTO)
    private Integer actionId;
    private String userId;
    private String actionType;
    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSONObject actionDetail;
    private LocalDateTime actionTime;
}
