package nnu.mnr.satellite.mapper.modeling;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import nnu.mnr.satellite.model.po.modeling.Methlib;
import nnu.mnr.satellite.model.vo.modeling.MethlibInfoVO;
import nnu.mnr.satellite.utils.typeHandler.JSONArrayTypeHandler;
import nnu.mnr.satellite.utils.typeHandlerEx.TagIdsTypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@DS("mysql-ard-dev")
public interface IMethlibRepo extends BaseMapper<Methlib> {
    @Select({
            "<script>",
            "SELECT m_t.*, GROUP_CONCAT(mt_t.tag_id) AS tag_ids FROM methlib_table m_t ",
            "LEFT JOIN methlib_tag_table mt_t ON m_t.id = mt_t.method_id ",
            "<where>",
            "   <if test='tags != null and tags.size() > 0'>",
            "       AND mt_t.tag_id IN ",
            "       <foreach collection='tags' item='tag' open='(' separator=',' close=')'>",
            "           #{tag}",
            "       </foreach>",
            "   </if>",
            "   <if test='searchText != null and searchText != \"\"'>",
            "       AND (m_t.name LIKE CONCAT('%', #{searchText}, '%') OR m_t.description LIKE CONCAT('%', #{searchText}, '%'))",
            "   </if>",
            "</where>",
            "GROUP BY m_t.id",
            "<if test='sortField != null and sortField != \"\"'>",
            "   ORDER BY ",
            "   <choose>",
            "       <when test=\"sortField == 'createTime'\">m_t.create_time</when>",
            "       <when test=\"sortField == 'longDesc'\">m_t.long_desc</when>",
            "       <otherwise>m_t.${sortField}</otherwise>",
            "   </choose>",
            "   <choose>",
            "       <when test='asc != null and asc'>ASC</when>",
            "       <when test='asc != null and !asc'>DESC</when>",
            "   </choose>",
            "</if>",
            "</script>"
    })
    @Results({
            @Result(property = "tagIds", column = "tag_ids", typeHandler = TagIdsTypeHandler.class),
            @Result(property = "params", column = "params", typeHandler = JSONArrayTypeHandler.class)
    })
    IPage<MethlibInfoVO> getMethlibInfo(Page<MethlibInfoVO> page,
                                        @Param("tags") List<Integer> tags,
                                        @Param("sortField") String sortField,
                                        @Param("asc") Boolean asc,
                                        @Param("searchText") String searchText);
}
