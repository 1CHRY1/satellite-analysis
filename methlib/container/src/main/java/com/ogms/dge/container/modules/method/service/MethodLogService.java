package com.ogms.dge.container.modules.method.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.modules.method.entity.MethodLogEntity;

import java.util.Map;

/**
 * 方法调用记录表
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-08-23 20:06:48
 */
public interface MethodLogService extends IService<MethodLogEntity> {

    PageUtils queryPage(Map<String, Object> params);

    int deleteBatchByMethodIds(Long[] methodIds);
}

