package nnu.mnr.satellite.model.po;

import com.alibaba.fastjson2.support.geo.Geometry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 21:16
 * @Description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(buildMethodName = "buildImage")
public class Image {
    private String imageId;
    private String productId;
    private String sensorId;
    private LocalDateTime datetime;
    private Integer tileLevelNum;
//    private HashSet<String> tileLevels;
    private String crs;
    private String band;
//    private Geometry bbox;
    private String tifPath;
    private String pngPath;
}
