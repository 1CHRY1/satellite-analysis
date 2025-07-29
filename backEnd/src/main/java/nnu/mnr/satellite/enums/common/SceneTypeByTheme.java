package nnu.mnr.satellite.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum SceneTypeByTheme {
    dem("dem", "DEM产品"),
    dsm("dsm", "DSM产品"),
    ndvi("ndvi", "NDVI产品"),
    threed("3d", "红绿立体影像"),
    svr("svr", "形变速率产品");

    private final String code;
    private final String label;

    /**
     * 获取所有分类的名称列表（用于前端 category 字段）
     */
    public static List<String> getCategoryNames() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static List<String> getAllCodes() {
        return Arrays.stream(values())
                .map(SceneTypeByTheme::getCode) // 使用 Lombok @Getter 生成的 getCode()
                .collect(Collectors.toList());
    }
}
