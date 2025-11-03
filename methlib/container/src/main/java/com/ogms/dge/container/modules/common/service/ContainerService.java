package com.ogms.dge.container.modules.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.modules.common.entity.ContainerEntity;

import java.util.Map;

/**
 * 
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2025-04-02 15:07:30
 */
public interface ContainerService extends IService<ContainerEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

