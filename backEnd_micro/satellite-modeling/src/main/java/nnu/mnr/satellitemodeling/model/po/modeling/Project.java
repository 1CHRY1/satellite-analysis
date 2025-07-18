package nnu.mnr.satellitemodeling.model.po.modeling;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/21 20:48
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("project")
public class Project {

    @TableId
    private String projectId;
    private String projectName;

    private String containerId;
    private String environment;
    private LocalDateTime createTime;
    private String dataBucket;

    private String workDir;
    private String pyPath;
    private String serverDir;
    private String serverPyPath;
    private String watchPath;
    private String outputPath;
    private String dataPath;

    private String pyContent;
    private HashSet<String> packages;

    private String createUser;
    private String createUserName;
    private String createUserEmail;
    private HashSet<String> joinedUsers;

    String description;
}
