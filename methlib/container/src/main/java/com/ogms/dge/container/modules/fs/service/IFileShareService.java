package com.ogms.dge.container.modules.fs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ogms.dge.container.modules.fs.entity.FileShare;
import com.ogms.dge.container.modules.fs.entity.dto.SessionShareDto;
import com.ogms.dge.container.modules.fs.entity.query.FileShareQuery;
import com.ogms.dge.container.modules.fs.entity.vo.PaginationResultVO;

/**
 * <p>
 * 分享信息 服务类
 * </p>
 *
 * @author jin wang
 * @since 2023-09-07
 */
public interface IFileShareService extends IService<FileShare> {

    PaginationResultVO findListByPage(FileShareQuery query);

    void saveShare(FileShare share);

    void deleteFileShareBatch(String[] split, Long userId);

    FileShare getFileShareByShareId(String shareId);

    SessionShareDto checkShareCode(String shareId, String code);
}
