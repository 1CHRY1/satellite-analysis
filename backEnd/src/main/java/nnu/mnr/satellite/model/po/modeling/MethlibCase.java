package nnu.mnr.satellite.model.po.modeling;

import com.alibaba.fastjson2.JSONObject;
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
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("methlib_case_table")
public class MethlibCase {
    @TableId
    private String caseId;
    private Integer methodId;
    private String status;
    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSONObject params;
    @TableField(typeHandler = FastJson2TypeHandler.class)
    private JSONObject result;
    private String userId;
    private LocalDateTime createTime;
}
