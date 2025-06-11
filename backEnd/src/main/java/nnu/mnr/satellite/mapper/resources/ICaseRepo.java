package nnu.mnr.satellite.mapper.resources;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.resources.Case;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
//分页
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @name: ICaseRepo
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 6/4/2025 8:08 PM
 * @version: 1.0
 */
//@Repository("ICaseRepo")
public interface ICaseRepo extends BaseMapper<Case> {

    @Insert("INSERT INTO case_table (case_id, case_name, resolution, boundary, scene_list, status, result) " +
            "VALUES (#{caseId}, #{caseName}, #{resolution}, ST_GeomFromText(#{boundary}, 4326, 'axis-order=long-lat'), #{sceneList}, #{status}, #{result})")
    int insertCase(Case caseObj);

    @Update("UPDATE case_table SET case_name = #{caseName}, resolution = #{resolution}, " +
            "boundary = ST_GeomFromText(#{boundary}, 4326, 'axis-order=long-lat'), " +
            "scene_list = #{sceneList}, status = #{status}, result = #{result} " +
            "WHERE case_id = #{caseId}")
    int updateCaseById(Case caseObj);

    // 分页查询方法
    @Select("<script>" +
            "SELECT * FROM case_table " +
            "<where>" +
            "   <if test='searchText != null and searchText != \"\"'>" +
            "       AND (case_name LIKE CONCAT('%', #{searchText}, '%') OR resolution LIKE CONCAT('%', #{searchText}, '%'))" +
            "   </if>" +
            "</where>" +
            "<if test='sortField != null and sortField != \"\"'>" +
            "   ORDER BY ${sortField}" +
            "   <if test='asc != null and asc'>" +
            "       ASC" +
            "   </if>" +
            "   <if test='asc != null and !asc'>" +
            "       DESC" +
            "   </if>" +
            "</if>" +
            "</script>")
    IPage<Case> selectPageWithCondition(
            Page<Case> page,
            @Param("searchText") String searchText,
            @Param("sortField") String sortField,
            @Param("asc") Boolean asc
    );
}
