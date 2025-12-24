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
    svr("svr", "形变速率产品"),
    lai("lai", "叶面积指数"),
    fvc("fvc", "植被覆盖度"),
    fpar("fpar", "植被层吸收光合有效辐射"),
    lst("lst", "地表温度"),
    lse("lse", "地表比辐射率"),
    npp("npp", "净初级生产力"),
    gpp("gpp", "总初级生产力"),
    et("et", "蒸散发"),
    wue("wue", "水分利用效率"),
    cue("cue", "碳利用效率"),
    esi("esi", "蒸散胁迫指数"),
    apar("apar", "吸收性光合有效辐射"),
    bba("bba", "黑空宽波段反照率"),
    aridity_index("aridity_index", "干燥度指数"),
    vcf("vcf", "树木覆盖度");

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
