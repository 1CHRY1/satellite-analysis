package nnu.mnr.satellite.model.vo.modeling;

import lombok.Data;

import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/29 14:54
 * @Description:
 */

@Data
public class ProjectVO {

    private String projectName;
    private String environment;
    private String createTime;
    private String packages;
    private String createUser;
    private HashSet<String> joinedUsers;
    private String description;

}
