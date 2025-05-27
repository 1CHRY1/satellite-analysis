package nnu.mnr.satellite.repository.geo;

import nnu.mnr.satellite.model.po.geo.GeoLocation;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/5/26 20:55
 * @Description:
 */
public interface LocationRepo {

    List<GeoLocation> searchByName(String keyword);

}
