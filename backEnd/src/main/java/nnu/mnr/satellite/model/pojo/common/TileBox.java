package nnu.mnr.satellite.model.pojo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Yiming
 * @Date: 2023/08/15/19:09
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TileBox {
    double xMin;
    double xMax;
    double yMin;
    double yMax;
    String name;
    Integer projection;
    String visualId;
    List<String> fieldList;
}
