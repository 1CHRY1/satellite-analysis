package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.dto.admin.image.ImagePathsDTO;
import nnu.mnr.satellite.model.po.resources.Image;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/11 17:26
 * @Description:
 */

//@Repository("ImageRepo")
public interface IImageRepo extends BaseMapper<Image> {

    @Select({
            "<script>",
            "SELECT bucket, tif_path ",
            "FROM image_table ",
            "WHERE scene_id IN ",
            "<foreach collection='sceneIds' item='id' open='(' separator=',' close=')'>",
            "   #{id}",
            "</foreach>",
            "</script>"
    })
    List<ImagePathsDTO> findImagePathsBySceneId(@Param("sceneIds") List<String> sceneIds);

}
