package nnu.mnr.satelliteresource.model.dto.common;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/25 15:24
 * @Description:
 */

@Data
@Builder
public class FileData {

    private String type;
    private byte[] stream;

}
