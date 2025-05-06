package nnu.mnr.satellitemodeling.model.vo.user;

import lombok.Data;

import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/29 15:21
 * @Description:
 */

@Data
public class UserVO {

    private String userId;
    private String userName;
    private String email;
    private String phone;
    private String province;
    private String city;
    private String title;
    private String organization;
    private String introduction;
    private String createTime;
    private HashSet<String> createdProjects;
    private HashSet<String> joinedProjects;

}
