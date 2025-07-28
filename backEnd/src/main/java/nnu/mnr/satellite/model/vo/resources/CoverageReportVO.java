package nnu.mnr.satellite.model.vo.resources;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CoverageReportVO {
    private Integer total; // 总数据量
    private String coverage; // 总体覆盖率（字符串，如 "98.99%"）
    private List<String> category; // 分类体系列表
    private Map<String, DatasetItemVO> dataset; // 分类数据集（键为分类名，如 "subMeter"）

    @Data
    public static class DatasetItemVO {
        private String label; // 分类显示名称（如 "亚米分辨率数据集"）
        private Float resolution; // 分辨率数值（如 0.5）
        private Integer total; // 该分类下的数据量
        private String coverage; // 该分类的覆盖率（如 "50.99%"）
        private List<Map<String, Object>> dataList; // 传感器列表
    }

}
