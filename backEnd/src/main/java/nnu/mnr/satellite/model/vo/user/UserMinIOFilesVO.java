package nnu.mnr.satellite.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMinIOFilesVO {
    private String name;
    private String path;
    private boolean isDir;
    private long size;
    private OffsetDateTime lastModified;
    private List<UserMinIOFilesVO> children;
}
