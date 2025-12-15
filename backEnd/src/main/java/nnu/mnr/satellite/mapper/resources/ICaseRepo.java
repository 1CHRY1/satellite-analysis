package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Case;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * @name: ICaseRepo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/4/2025 8:08 PM
 * @version: 1.0
 */
//@Repository("ICaseRepo")
public interface ICaseRepo extends BaseMapper<Case> {

    @Insert("INSERT INTO case_table (case_id, address, region_id, resolution, boundary, scene_list, data_set, band_list, status, result, create_time, type, user_id) " +
            "VALUES (#{caseId}, #{address}, #{regionId}, #{resolution}, ST_GeomFromText(#{boundary}, 4326, 'axis-order=long-lat'), #{sceneList}, #{dataSet}, #{bandList}, #{status}, #{result}, #{createTime}, #{type}, #{userId})")
    int insertCase(Case caseObj);

    @Update("UPDATE case_table SET address = #{address}, region_id = #{regionId}, resolution = #{resolution}, " +
            "boundary = ST_GeomFromText(#{boundary}, 4326, 'axis-order=long-lat'), " +
            "scene_list = #{sceneList}, data_set = #{dataSet}, band_list = #{bandList}, status = #{status}, result = #{result}, create_time = #{createTime} " +
            "WHERE case_id = #{caseId}")
    int updateCaseById(Case caseObj);
}
