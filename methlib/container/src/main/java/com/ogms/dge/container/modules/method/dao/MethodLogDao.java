package com.ogms.dge.container.modules.method.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.method.entity.MethodLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 方法调用记录表
 *
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-08-23 20:06:48
 */
@Mapper
public interface MethodLogDao extends BaseMapper<MethodLogEntity> {
    /**
     * 根据方法ID数组，批量删除
     */
    int deleteBatchByMethodIds(Long[] methodIds);
}
