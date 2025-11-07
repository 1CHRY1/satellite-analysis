package com.ogms.dge.workspace.modules.project.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ogms.dge.workspace.modules.project.entity.ProjectEntity;
import com.ogms.dge.workspace.modules.project.vo.ProjectVo;
import com.ogms.dge.workspace.modules.workspace.entity.WsEntity;
import com.ogms.dge.workspace.modules.workspace.vo.WsVo;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @name: ProjectConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 11/8/2024 9:09 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectConverter {

    @Mapping(target = "createUserName", ignore = true)
    ProjectVo po2Vo(ProjectEntity project) throws JsonProcessingException;

    @AfterMapping
    default void handleCustomFields(ProjectEntity project, @MappingTarget ProjectVo projectVo) throws JsonProcessingException {
        // TODO TEMP PROCESS
        projectVo.setCreateUserName("admin");
    }

    default List<ProjectVo> poList2VoList(List<ProjectEntity> projects) {
        return projects.stream()
                .map(project -> {
                    try {
                        return po2Vo(project);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
