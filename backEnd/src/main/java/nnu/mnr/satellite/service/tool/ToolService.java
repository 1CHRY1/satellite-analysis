package nnu.mnr.satellite.service.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellite.mapper.modeling.IProjectRepo;
import nnu.mnr.satellite.mapper.tool.IToolRepo;
import nnu.mnr.satellite.mapper.user.IUserRepo;
import nnu.mnr.satellite.model.dto.modeling.ProjectBasicDTO;
import nnu.mnr.satellite.model.dto.tool.*;
import nnu.mnr.satellite.model.po.modeling.Project;
import nnu.mnr.satellite.model.po.resources.Case;
import nnu.mnr.satellite.model.po.tool.*;
import nnu.mnr.satellite.model.pojo.modeling.DockerServerProperties;
import nnu.mnr.satellite.model.vo.common.CommonResultVO;
import nnu.mnr.satellite.model.vo.modeling.ServicePublishVO;
import nnu.mnr.satellite.model.vo.modeling.ServiceStatusVO;
import nnu.mnr.satellite.model.vo.resources.CaseInfoVO;
import nnu.mnr.satellite.model.vo.tool.*;
import nnu.mnr.satellite.service.common.DockerService;
import nnu.mnr.satellite.service.common.SftpDataService;
import nnu.mnr.satellite.service.modeling.ProjectDataService;
import nnu.mnr.satellite.service.websocket.ModelSocketService;
import nnu.mnr.satellite.utils.common.IdUtil;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@DS("mysql-ard-dev")
public class ToolService {

    @Autowired
    IToolRepo toolRepo;

    @Autowired
    IUserRepo userRepo;

    @Autowired
    IProjectRepo projectRepo;

    @Autowired
    ModelMapper toolModelMapper;

    @Autowired
    DockerServerProperties dockerServerProperties;
    @Autowired
    DockerService dockerService;
    @Autowired
    ProjectDataService projectDataService;
    @Autowired
    SftpDataService sftpDataService;
    @Autowired
    ModelSocketService modelSocketService;


    public CommonResultVO publishTool(Code2ToolDTO code2ToolDTO) {

        String toolId = IdUtil.generateToolId();
        String toolName = code2ToolDTO.getToolName();
        Integer servicePort = code2ToolDTO.getServicePort();
        String description = code2ToolDTO.getDescription();
        List<String> tags = Optional.ofNullable(code2ToolDTO.getTags()).orElse(Collections.emptyList());
        Integer categoryId = code2ToolDTO.getCategoryId();
        List<JSONObject> parameters = Optional.ofNullable(code2ToolDTO.getParameters()).orElse(Collections.emptyList());
        boolean share = code2ToolDTO.isShare();
        String userId = Optional.ofNullable(code2ToolDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("userId 不能为空"));
        String projectId = Optional.ofNullable(code2ToolDTO.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("projectID 不能为空"));

        // 校验用户和项目是否存在
        if (!userRepo.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
        if (!projectRepo.existsById(projectId)) {
            throw new IllegalArgumentException("工程不存在: " + projectId);
        }

        if (isProjectIdExistInTool(projectId) != null) {
            throw new IllegalArgumentException("程序对应工具已存在: " + isProjectIdExistInTool(projectId));
        }

        // 发布工具并插入数据库
        try {
            int hostPort;
            int internalPort = dockerServerProperties.getServiceDefaults().getInternalPort();
            Project project = projectDataService.getProjectById(projectId);

            if (servicePort != null) {
                // Validate provided port is in range
                int startPort = dockerServerProperties.getServicePortRange().getStart();
                int endPort = dockerServerProperties.getServicePortRange().getEnd();

                if (servicePort < startPort || servicePort > endPort) {
                    throw new RuntimeException("Port " + servicePort + " is outside allowed range " + startPort + "-" + endPort);
                }

                if (!dockerService.isHostPortFree(servicePort)) {
                    throw new RuntimeException("Port " + servicePort + " is already in use");
                }

                hostPort = servicePort;
            } else {
                // Auto-assign port
                hostPort = dockerService.findNextFreePort(
                        dockerServerProperties.getServicePortRange().getStart(),
                        dockerServerProperties.getServicePortRange().getEnd()
                );
            }

            // Check if container needs to be recreated with port binding
            String containerId = project.getContainerId();
            boolean needsRecreation = true; // For simplicity, always recreate for now

            containerId = dockerService.recreateProjectContainerWithPort(project, hostPort, internalPort);
            project.setContainerId(containerId);
            projectRepo.updateById(project);

            // Install Flask if not already installed
            String installFlaskCommand = "pip show flask || pip install flask";
            dockerService.runCMDInContainerSilent(containerId, installFlaskCommand);

            // Start Flask application in background
            String flaskCommand = "nohup python -u " + project.getPyPath() + " > service.out 2>&1 &";
            dockerService.runCMDInContainerSilent(containerId, flaskCommand);

            // Create service metadata
            String dockerHost = dockerServerProperties.getDefaultServer().get("host");
            String url = "http://" + dockerHost + ":" + hostPort + "/";

            // Save service metadata
            String serviceDir = project.getServerDir() + dockerServerProperties.getFaas().getWorkDirSubdir() + "/";
            sftpDataService.createRemoteDir(serviceDir, 0755);

            JSONObject faasMetadata = new JSONObject();
            faasMetadata.put("projectId", projectId);
            faasMetadata.put("host", dockerHost);
            faasMetadata.put("port", hostPort);
            faasMetadata.put("internalPort", internalPort);
            faasMetadata.put("url", url);
            faasMetadata.put("running", true);
            faasMetadata.put("publishedAt", LocalDateTime.now().toString());

            String faasPath = serviceDir + "faas.json";
            sftpDataService.writeRemoteFile(faasPath, faasMetadata.toString());

            // Notify via WebSocket
            modelSocketService.sendMessageByProject(userId, projectId, "Service running at: " + url);

            Tool toolObj = Tool.builder()
                    .toolId(toolId)
                    .userId(userId)
                    .projectId(projectId)
                    .toolName(toolName)
                    .description(description)
                    .tags(tags)
                    .categoryId(categoryId)
                    .parameters(parameters.toString())
                    .isShare(share)
                    .isPublish(true)
                    .build();
            toolRepo.insertTool(toolObj);

            Map<String, Object> data = new HashMap<>();
            data.put("toolId", toolId);
            data.put("url", url);

            return CommonResultVO.builder()
                    .status(1)
                    .message("工具发布成功")
                    .data(data)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("工具发布失败: " + e.getMessage(), e);
        }
    }

    public CommonResultVO updateToolById(ToolInfoDTO toolInfoDTO){

        String toolId = toolInfoDTO.getToolId();
        if(toolId == null || toolId.isEmpty()){
            throw new IllegalArgumentException("toolId不能为空");
        }
        String toolName = toolInfoDTO.getToolName();
        String description = toolInfoDTO.getDescription();
        List<String> tags = Optional.ofNullable(toolInfoDTO.getTags()).orElse(Collections.emptyList());
        Integer categoryId = toolInfoDTO.getCategoryId();
        List<JSONObject> parameters = Optional.ofNullable(toolInfoDTO.getParameters()).orElse(Collections.emptyList());
        boolean share = toolInfoDTO.isShare();
        if(isToolIdExistInTool(toolId)){
            Tool toolObj = Tool.builder()
                    .toolId(toolId)
                    .toolName(toolName)
                    .description(description)
                    .tags(tags)
                    .categoryId(categoryId)
                    .parameters(parameters.toString())
                    .isShare(share)
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

    public CommonResultVO unpublishTool(ToolBasicDTO toolBasicDTO) {
        String userId = toolBasicDTO.getUserId();
        String toolId = toolBasicDTO.getToolId();
        Tool tool = toolRepo.selectById(toolId);
        String projectId = tool.getProjectId();

        // Validate user and project
        if (!projectDataService.VerifyUserProject(userId, projectId)) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("User " + userId + " Can't Operate Project " + projectId)
                    .build();
        }

        Project project = projectDataService.getProjectById(projectId);
        if (project == null) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("Project " + projectId + " hasn't been Registered")
                    .build();
        }

        try {
            String containerId = project.getContainerId();

            // Stop Flask process
            String stopCommand = "pkill -f \"python.*" + project.getPyPath().replace("/", "\\/") + "\"";
            dockerService.runCMDInContainerSilent(containerId, stopCommand);

            // Update service metadata
            String serviceDir = project.getServerDir() + dockerServerProperties.getFaas().getWorkDirSubdir() + "/";
            String faasPath = serviceDir + "faas.json";

            try {
                String existingMetadata = sftpDataService.readRemoteFile(faasPath);
                JSONObject faasData = JSONObject.parseObject(existingMetadata);
                faasData.put("running", false);
                faasData.put("stoppedAt", LocalDateTime.now().toString());
                sftpDataService.writeRemoteFile(faasPath, faasData.toString());
            } catch (Exception e) {
                log.warn("Failed to update service metadata: {}", e.getMessage());
            }

            // Notify via WebSocket
            modelSocketService.sendMessageByProject(userId, projectId, "Service stopped");
            tool.setPublish(false);
            toolRepo.updateToolById(tool);
            return CommonResultVO.builder()
                    .status(1)
                    .message("工具服务停止成功")
                    .build();

        } catch (Exception e) {
            log.error("Failed to unpublish service for project {}: {}", projectId, e.getMessage());
            JSONObject projectIdJSON = new JSONObject();
            projectIdJSON.put("projectId", projectId);
            return CommonResultVO.builder()
                    .status(-1)
                    .message("Failed to stop service: " + e.getMessage())
                    .data(projectIdJSON)
                    .build();
        }
    }

    public CommonResultVO deleteToolById(String toolId){
        if(toolId == null || toolId.isEmpty()){
            throw new IllegalArgumentException("toolId不能为空");
        }
        if(isToolIdExistInTool(toolId)){
            boolean publish = toolRepo.selectById(toolId).isPublish();
            if (publish) {
                return CommonResultVO.builder()
                        .status(-1)
                        .message("工具删除失败，请先停止服务")
                        .build();
            }
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

    public CommonResultVO getServiceStatus(ToolBasicDTO toolBasicDTO) {
        String userId = toolBasicDTO.getUserId();
        String toolId = toolBasicDTO.getToolId();
        Tool tool = toolRepo.selectById(toolId);
        String projectId = tool.getProjectId();
        ServiceStatusVO serviceStatusVO;

        // Validate user and project
        if (!projectDataService.VerifyUserProject(userId, projectId)) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("服务信息查询失败，该工具不属于你")
                    .build();
        }

        Project project = projectDataService.getProjectById(projectId);
        if (project == null) {
            return CommonResultVO.builder()
                    .status(-1)
                    .message("服务信息查询失败，该工具不存在")
                    .build();
        }

        try {
            String serviceDir = project.getServerDir() + dockerServerProperties.getFaas().getWorkDirSubdir() + "/";
            String faasPath = serviceDir + "faas.json";

            try {
                String metadata = sftpDataService.readRemoteFile(faasPath);
                JSONObject faasData = JSONObject.parseObject(metadata);

                // Check if process is actually running
                String containerId = project.getContainerId();
                String checkCommand = "pgrep -f \"python.*" + project.getPyPath().replace("/", "\\/") + "\"";
                String status = "stopped";

                try {
                    // This is a simplified check - in a real implementation you'd capture the command output
                    dockerService.runCMDInContainerSilent(containerId, checkCommand);
                    status = faasData.getBooleanValue("running") ? "running" : "stopped";
                } catch (Exception e) {
                    return CommonResultVO.builder()
                            .status(-1)
                            .message("服务信息查询失败，查询容器出错")
                            .build();
                }

                serviceStatusVO = ServiceStatusVO.builder()
                        .status(status)
                        .url(faasData.getString("url"))
                        .build();

            } catch (Exception e) {
                // No metadata file means not published
                return CommonResultVO.builder()
                        .status(-1)
                        .message("服务信息查询失败，没有元数据，可能未发布")
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to get service status for project {}: {}", projectId, e.getMessage());
            return CommonResultVO.builder()
                    .status(-1)
                    .message("服务信息查询失败，无法获取工具状态")
                    .build();
        }
        return CommonResultVO.builder()
                .status(1)
                .message("服务信息查询成功")
                .data(serviceStatusVO)
                .build();
    }

    public CommonResultVO getToolById(String toolId) {
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

        return CommonResultVO.builder()
                .status(1)
                .message("工具信息查询成功")
                .data(toolInfoVO)
                .build();
    }

    public CommonResultVO getToolPage(ToolPageDTO toolPageDTO) {
        // 构造分页对象
        Page<Tool> page = new Page<>(toolPageDTO.getPage(), toolPageDTO.getPageSize());

        // 排序
        String sortField = toolPageDTO.getSortField();
        Boolean asc = toolPageDTO.getAsc();
        LambdaQueryWrapper<Tool> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (toolPageDTO.getUserId() != null) {
            lambdaQueryWrapper.eq(Tool::getUserId, toolPageDTO.getUserId());
        }
        if (sortField != null && !sortField.isEmpty()) {
            // 使用 sortField 对应的数据库字段进行排序
            switch (sortField) {
                case "createTime":
                    lambdaQueryWrapper.orderBy(true, asc, Tool::getCreateTime);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sort field: " + sortField);
            }
        }
        // 查询，lambdaQueryWrapper没有显式指定selecet，默认select *
        IPage<Tool> toolPage = toolRepo.selectPage(page, lambdaQueryWrapper);

        return CommonResultVO.builder()
                .status(1)
                .message("分页查询成功")
                .data(mapPage(toolPage))
                .build();
    }

    // 3. 执行工具
    public CommonResultVO executeTool(ExecutionInputDTO executionInputDTO) {

        return null;
    }

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

    private IPage<ToolInfoVO> mapPage(IPage<Tool> toolPage) {
        // 映射记录列表
        Type destinationType = new TypeToken<List<ToolInfoVO>>() {
        }.getType();
        List<ToolInfoVO> toolInfoVOList = toolModelMapper.map(toolPage.getRecords(), destinationType);

        // 创建一个新的 Page 对象
        Page<ToolInfoVO> resultPage = new Page<>();
        resultPage.setRecords(toolInfoVOList);
        resultPage.setTotal(toolPage.getTotal());
        resultPage.setSize(toolPage.getSize());
        resultPage.setCurrent(toolPage.getCurrent());

        return resultPage;
    }

}
