package nnu.mnr.satellite.model.po.modeling;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("tag_table")
public class Tag {
    @TableId
    private Integer id;
    private String name;
    private String createTime;
}
