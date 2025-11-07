package com.ogms.dge.container.modules.common.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ogms.dge.container.modules.common.entity.UploadRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上传记录
 * 
 * @author Lingkai Shi
 * @email lingkai.shi@nnu.edu.cn
 * @date 2024-09-27 14:33:42
 */
@Mapper
public interface UploadRecordDao extends BaseMapper<UploadRecordEntity> {
	
}
