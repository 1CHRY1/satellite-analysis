package nnu.mnr.satellite.model.dto.admin.cache;

import lombok.Data;

import java.util.List;

@Data
public class DeleteRedisCacheDTO {
    private List<String> keys;
}
