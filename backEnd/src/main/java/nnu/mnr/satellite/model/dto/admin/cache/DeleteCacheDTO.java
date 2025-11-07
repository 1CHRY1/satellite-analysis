package nnu.mnr.satellite.model.dto.admin.cache;

import lombok.Data;

import java.util.List;

@Data
public class DeleteCacheDTO {
    private List<DeleteCache> cacheKeys;

    @Data
    public static class DeleteCache {
        private String type;
        private String cacheKey;
    }
}
