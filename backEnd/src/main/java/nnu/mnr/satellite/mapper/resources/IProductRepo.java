package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import nnu.mnr.satellite.model.po.resources.Product;
import nnu.mnr.satellite.model.vo.admin.ProductInfoWithSensorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:25
 * @Description:
 */

@Mapper
public interface IProductRepo extends BaseMapper<Product>  {
    @Select({
            "<script>",
            "SELECT EXISTS (",
            "   SELECT 1 FROM product_table WHERE sensor_id IN",
            "   <foreach collection='sensorIds' item='id' open='(' separator=',' close=')'>",
            "       #{id}",
            "   </foreach>",
            ")",
            "</script>"
    })
    boolean existsBySensorIds(@Param("sensorIds") List<String> sensorIds);

    @Select("SELECT p.*, s.sensor_name AS sensorName " +
            "FROM product_table p " +
            "LEFT JOIN sensor_table s ON p.sensor_id = s.sensor_id " +
            "${ew.customSqlSegment}")
    IPage<ProductInfoWithSensorVO> selectProductPageWithSensorName(Page<Product> page, @Param(Constants.WRAPPER) Wrapper<Product> wrapper);
}

