package com.ogms.dge.container.modules.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.modules.common.entity.UploadRecordEntity;
import com.ogms.dge.container.modules.common.enums.UploadRecordStatusEnum;
import com.ogms.dge.container.modules.common.enums.UploadRecordTypeEnum;

import java.util.Map;

/**
 * 上传记录
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-27 14:33:42
 */
public interface UploadRecordService extends IService<UploadRecordEntity> {

    PageUtils queryPage(Map<String, Object> params);

    UploadRecordEntity saveOrUpdate(Long id, Long userId, String packageName, UploadRecordTypeEnum type, UploadRecordStatusEnum status, String uuid, String description);

    void delete(Long[] ids);
}

