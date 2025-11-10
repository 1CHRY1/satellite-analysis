package com.ogms.dge.container.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.sys.entity.SysUserTokenEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户Token
 */
@Mapper
public interface SysUserTokenDao extends BaseMapper<SysUserTokenEntity> {

    SysUserTokenEntity queryByToken(String token);
	
}
