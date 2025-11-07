package nnu.mnr.satellite.model.vo.admin;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatsReportVO {
    private OverallVO overall;
    private DataVO data;
    private TaskVO task;
    private StorageVO storage;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OverallVO {
        private Long data;
        private Long user;
        private Long task;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DataVO {
        private List<Info> satelliteList;
        private List<Info> vectorList;
        private List<Info> themeList;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Info {
            private String key;
            private String label;
            private Long value;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TaskVO {
        private Long total;
        private Long completed;
        private Long error;
        private Long running;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StorageVO {
        private BigDecimal totalSpace;
        private BigDecimal availSpace;
        private BigDecimal usedSpace;
        private List<BucketInfo> bucketsInfo;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class BucketInfo {
            private String bucketName;
            private Long bucketUsedSize;  // 已用空间
            private Long objectsCount;  // 文件数量
        }
    }
}
