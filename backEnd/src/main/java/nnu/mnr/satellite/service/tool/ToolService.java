package nnu.mnr.satellite.service.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.modeling.IProjectRepo;
import nnu.mnr.satellite.mapper.tool.IToolRepo;
import nnu.mnr.satellite.mapper.user.IUserRepo;
import nnu.mnr.satellite.model.dto.tool.*;
import nnu.mnr.satellite.model.po.tool.*;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.tool.*;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ToolService {

    @Autowired
    IToolRepo toolRepo;

    @Autowired
    IUserRepo userRepo;

    @Autowired
    IProjectRepo projectRepo;

    public CommonResultVO publishTool(Code2ToolDTO code2ToolDTO) {

        String toolId = IdUtil.generateToolId();
        String environment = code2ToolDTO.getEnvironment();
        String toolName = code2ToolDTO.getToolName();
        String description = code2ToolDTO.getDescription();
        List<String> tags = Optional.ofNullable(code2ToolDTO.getTags()).orElse(Collections.emptyList());
        String category = code2ToolDTO.getCategory();
        List<JSONObject> parameters = Optional.ofNullable(code2ToolDTO.getParameters()).orElse(Collections.emptyList());
        String userId = Optional.ofNullable(code2ToolDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("userId 不能为空"));
        String projectId = Optional.ofNullable(code2ToolDTO.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("projectID 不能为空"));

        // 校验用户和项目是否存在
        if (!userRepo.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        if (!projectRepo.existsById(projectId)) {
            throw new IllegalArgumentException("工具不存在: " + projectId);
        }

        if (isProjectIdExistInTool(projectId) != null) {
            throw new IllegalArgumentException("程序对应工具已存在: " + isProjectIdExistInTool(projectId));
        }


        Tool toolObj = Tool.builder()
                .toolId(toolId)
                .projectId(projectId)
                .environment(environment)
                .userId(userId)
                .toolName(toolName)
                .description(description)
                .tags(tags)
                .category(category)
                .parameters(parameters.toString())
                .build();

        // 插入数据库
        try {
            toolRepo.insertTool(toolObj);
        } catch (Exception e) {
            throw new RuntimeException("工具发布失败: " + e.getMessage(), e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("toolId", toolId);
        return CommonResultVO.builder()
                .status(1)
                .message("工具发布成功")
                .data(data)
                .build();
    }

    public CommonResultVO updateToolById(ToolInfoDTO toolInfoDTO){

        String toolId = toolInfoDTO.getToolId();
        if(toolId == null || toolId.isEmpty()){
            throw new IllegalArgumentException("toolId不能为空");
        }
        String environment = toolInfoDTO.getEnvironment();
        String toolName = toolInfoDTO.getToolName();
        String description = toolInfoDTO.getDescription();
        List<String> tags = Optional.ofNullable(toolInfoDTO.getTags()).orElse(Collections.emptyList());
        String category = toolInfoDTO.getCategory();
        List<JSONObject> parameters = Optional.ofNullable(toolInfoDTO.getParameters()).orElse(Collections.emptyList());
        if(isToolIdExistInTool(toolId)){
            Tool toolObj = Tool.builder()
                    .toolId(toolId)
                    .environment(environment)
                    .toolName(toolName)
                    .description(description)
                    .tags(tags)
                    .category(category)
                    .parameters(parameters.toString())
                    .build();
            // 更新数据库
            try {
                toolRepo.updateToolById(toolObj);
            } catch (Exception e) {
                throw new RuntimeException("工具修改失败: " + e.getMessage(), e);
            }
        }else {
            throw new IllegalArgumentException("工具不存在");
        }

        return CommonResultVO.builder()
                .status(1)
                .message("工具修改成功")
                .build();
    }

    public CommonResultVO deleteToolById(String toolId){
        if(toolId == null || toolId.isEmpty()){
            throw new IllegalArgumentException("toolId不能为空");
        }
        if(isToolIdExistInTool(toolId)){
            Tool toolObj = Tool.builder().toolId(toolId).build();
            try {
                toolRepo.deleteToolById(toolObj);
            } catch (Exception e) {
                throw new RuntimeException("工具删除失败: " + e.getMessage(), e);
            }
        }else {
            throw new IllegalArgumentException("该工具不存在");
        }
        return CommonResultVO.builder()
                .status(1)
                .message("工具删除成功")
                .build();
    }

    public ToolInfoVO getToolById(String toolId) {
        QueryWrapper<Tool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tool_id", toolId);
        Tool tool = toolRepo.selectOne(queryWrapper);
        if (tool == null) {
            throw new RuntimeException("Tool not found with id: " + toolId);
        }
        ToolInfoVO toolInfoVO = new ToolInfoVO();
        BeanUtils.copyProperties(tool, toolInfoVO);
        // 手动处理特殊字段（String parameters → List<JSONObject> parameters）
        if (tool.getParameters() != null) {
            List<JSONObject> parametersList = new ArrayList<>();
            try {
                // 假设 tool.getParameters() 是 JSON 数组字符串，如 "[{\"key\":\"value\"}, ...]"
                JSONArray jsonArray = JSONArray.parseArray(tool.getParameters());
                for (Object obj : jsonArray) {
                    parametersList.add((JSONObject) obj);
                }
                toolInfoVO.setParameters(parametersList);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse parameters", e);
            }
        }
        return toolInfoVO;
    }

    public List<ToolInfoVO> getAllTool() {
        // 1. 查询所有工具
        List<Tool> tools = toolRepo.selectList(new QueryWrapper<>());
        // 2. 转换为 ToolInfoVO 列表
        List<ToolInfoVO> toolList = new ArrayList<>();
        for (Tool tool : tools) {
            ToolInfoVO toolInfoVO = new ToolInfoVO();
            // 复制普通字段
            BeanUtils.copyProperties(tool, toolInfoVO);

            // 处理 parameters 字段（String → List<JSONObject>）
            if (tool.getParameters() != null) {
                try {
                    toolInfoVO.setParameters(JSONArray.parseArray(tool.getParameters(), JSONObject.class));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse parameters for toolId: " + tool.getToolId(), e);
                }
            }

            toolList.add(toolInfoVO);
        }

        return toolList;
    }

//    // 3. 执行工具
//    public ExecutionResultVO executeTool(String toolId, ExecutionInputDTO input) {
//        // 调用运行时环境（如Docker容器）执行代码
//        // 返回执行结果（日志、输出等）
//    }

    public String isProjectIdExistInTool(String projectId) {
        QueryWrapper<Tool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId).select("tool_id"); // 构造查询条件
        Tool tool = toolRepo.selectOne(queryWrapper); // 返回 Tool 对象（可能为 null）
        return tool != null ? tool.getToolId() : null; // 提取 toolId 或返回 null
    }

    public boolean isToolIdExistInTool(String toolId) {
        QueryWrapper<Tool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tool_Id", toolId); // 构造查询条件
        return toolRepo.selectCount(queryWrapper) > 0;
    }

}
