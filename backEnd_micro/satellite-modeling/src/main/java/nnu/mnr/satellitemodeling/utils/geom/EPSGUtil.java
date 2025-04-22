package nnu.mnr.satellitemodeling.utils.geom;

import org.opengis.referencing.FactoryException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/14 9:22
 * @Description:
 */
public class EPSGUtil {

    public static String getEPSGName(String code) throws FactoryException {
        String epsgCode = "EPSG:" + code;
        return epsgCode;
//        CoordinateReferenceSystem crs = CRS.decode(epsgCode);
//        return crs.getName().toString();
    }

}
