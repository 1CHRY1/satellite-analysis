package nnu.mnr.satellite.model.vo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nnu.mnr.satellite.enums.common.FileType;
import org.apache.tomcat.jni.FileInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/18 19:59
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "DFileInfoVOBuilder")
public class DFileInfoVO {

    private String fileName;
    private String filePath;
    private String absolutePath;
    private Long fileSize;
    private Date updateTime;
    private List<FileInfo> childrenFileList = new ArrayList<>();
    private FileType fileType;

}
