package nnu.mnr.satellite.model.po.common;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 20:43
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user")
public class User {

    @TableId
    private String userId;
    private String userName;
    private String password;
    private String email;

    private String phone;
    private String province;
    private String city;
    private String title;
    private String organization;
    private String introduction;
    private LocalDateTime createdTime;

    private String role;
    private HashSet<String> joinedProjects;
    private HashSet<String> createdProjects;

}
