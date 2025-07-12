package nnu.mnr.satellite.mapper.tool;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nnu.mnr.satellite.model.po.tool.Tool;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

//@Repository("IToolRepo")
@DS("pg_satellite")
public interface IToolRepo extends BaseMapper<Tool> {
    @Insert("INSERT INTO tool.tool_table (tool_id, project_id, environment, user_id, tool_name, description, tags, category, parameters, output_type) " +
            "VALUES (#{toolId}, #{projectId}, #{environment}, #{userId}, #{toolName}, #{description}, #{tags}, #{category}, #{parameters}, #{outputType})")
    int insertTool(Tool toolObj);

    @Update("UPDATE tool.tool_table SET environment = #{environment}, tool_name = #{toolName}, description = #{description}, output_type = #{outputType}" +
            "tags = #{tags}, category = #{category}, parameters = #{parameters} " +
            "WHERE tool_id = #{toolId}")
    int updateToolById(Tool toolObj);

    @Delete("DELETE FROM tool.tool_table WHERE tool_id = #{toolId}")
    int deleteToolById(Tool toolObj);
}
