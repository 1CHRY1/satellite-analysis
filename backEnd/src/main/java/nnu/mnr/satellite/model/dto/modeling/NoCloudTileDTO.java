package nnu.mnr.satellite.model.dto.modeling;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 无云一版图瓦片请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoCloudTileDTO {
    
    /**
     * 影像场景ID列表
     */
    private List<String> sceneIds;
} 