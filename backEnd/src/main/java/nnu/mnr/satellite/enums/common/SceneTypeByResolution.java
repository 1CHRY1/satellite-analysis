package nnu.mnr.satellite.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum SceneTypeByResolution {
    subMeter("亚米分辨率数据集", 0.5),
    twoMeter("2米分辨率数据集", 2.0),
    tenMeter("10米分辨率数据集", 10.0),
    thirtyMeter("30米分辨率数据集", 30.0),
    other("其他分辨率数据集", 999.0); // 999 表示最低优先级

    private final String label;
    private final double resolution; // 用于排序

    /**
     * 获取所有分类的名称列表（用于前端 category 字段）
     */
    public static List<String> getCategoryNames() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
