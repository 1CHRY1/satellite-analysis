package nnu.mnr.satellite.model.po.geo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/26 20:21
 * @Description:
 */

@Data
@Document(indexName = "locations")
public class GeoLocation {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text)
    private String address;

    @Field(type = FieldType.Text)
    private String typecode;

    @Field(type = FieldType.Text)
    private String pcode;

    @Field(type = FieldType.Text)
    private String pname;

    @Field(type = FieldType.Text)
    private String citycode;

    @Field(type = FieldType.Text)
    private String cityname;

    @Field(type = FieldType.Text)
    private String adcode;

    @Field(type = FieldType.Text)
    private String adname;

    @Field(type = FieldType.Text)
    private String tel;

    @Field(type = FieldType.Text, name = "GCJ02_经")
    private String gcj02Lon;

    @Field(type = FieldType.Text, name = "GCJ02_纬")
    private String gcj02Lat;

    @Field(type = FieldType.Text, name = "wgs84_经")
    private String wgs84Lon;

    @Field(type = FieldType.Text, name = "wgs84_纬")
    private String wgs84Lat;

    // GeoJSON 格式 geometry 字段
    @Field(type = FieldType.Object)
    private Geometry geometry;

    // 可选：完整 geojson feature 对象
//    @Field(type = FieldType.Object)
//    private Map<String, Object> geojson;

    @Field(type = FieldType.Text, name = "一级类")
    private String level1;

    @Field(type = FieldType.Text, name = "二级类")
    private String level2;

    @Field(type = FieldType.Text, name = "三级类")
    private String level3;

    @Data
    public static class Geometry {
        private String type;
        private List<Double> coordinates; // 0: lon, 1: lat
    }
}
