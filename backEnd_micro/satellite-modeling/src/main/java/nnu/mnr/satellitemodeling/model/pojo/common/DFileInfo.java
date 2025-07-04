package nnu.mnr.satellitemodeling.model.pojo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellitemodeling.enums.FileType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/18 20:06
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "DFileInfoBuilder")
public class DFileInfo {

    private String fileName;
    private String filePath;
    private String serverPath;
    private Long fileSize;
    private Date updateTime;
    private List<DFileInfo> childrenFileList = new ArrayList<>();
    private FileType fileType;

}
