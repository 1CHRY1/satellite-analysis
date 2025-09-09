package nnu.mnr.satellite.model.po.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("role_table")
public class Role {

    @TableId(type = IdType.AUTO)
    private Integer roleId;
    private String name;
    private String description;
    private Integer maxCpu;
    private Integer maxStorage;
    private Integer maxJob;
    private Integer isSuperAdmin;
}
