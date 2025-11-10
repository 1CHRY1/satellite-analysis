package com.ogms.dge.container.modules.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.common.utils.PageUtils;
import com.ogms.dge.container.modules.common.entity.DataSourceEntity;

import java.util.Map;

/**
 * 数据源类型
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-23 15:23:47
 */
public interface DataSourceService extends IService<DataSourceEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

