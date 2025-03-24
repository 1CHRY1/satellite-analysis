package nnu.mnr.satellite.model.pojo.common;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/17 14:05
 * @Description:
 */

@Data
@Builder(buildMethodName = "BuildSftpConn")
public class SftpConn {

    private String host;
    private String username;
    private String password;

}
