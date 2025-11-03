package com.ogms.dge.container.modules.common.converter;

import com.ogms.dge.container.modules.common.entity.UploadRecordEntity;
import com.ogms.dge.container.modules.common.vo.UploadRecordVo;
import com.ogms.dge.container.modules.data.service.ServiceService;
import com.ogms.dge.container.modules.sys.service.SysUserService;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @name: UploadRecordConverter
 * @description: TODO
 * @author: Lingkai Shi
 * @date: 9/27/2024 2:45 PM
 * @version: 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UploadRecordConverter {

    @Mapping(target = "username", ignore = true)
    UploadRecordVo po2Vo(UploadRecordEntity entity, @Context SysUserService sysUserService);

    default List<UploadRecordVo> poList2VoList(List<UploadRecordEntity> recordEntityList, @Context SysUserService userService) {
        return recordEntityList.stream()
                .map(record -> po2Vo(record, userService))
                .collect(Collectors.toList());
    }

    @AfterMapping
    default void handleCustomFields(UploadRecordEntity recordEntity, @MappingTarget UploadRecordVo recordVo, @Context SysUserService userService) {
        recordVo.setUsername(userService.getById(recordEntity.getUserId()).getUsername());
    }
}
