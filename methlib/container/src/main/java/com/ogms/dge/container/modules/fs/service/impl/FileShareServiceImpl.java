package com.ogms.dge.container.modules.fs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ogms.dge.container.common.exception.BusinessException;
import com.ogms.dge.container.common.utils.Constant;
import com.ogms.dge.container.common.utils.CopyTools;
import com.ogms.dge.container.common.utils.DateUtil;
import com.ogms.dge.container.common.utils.StringTools;
import com.ogms.dge.container.modules.fs.entity.FileInfo;
import com.ogms.dge.container.modules.fs.entity.FileShare;
import com.ogms.dge.container.modules.fs.entity.dto.FileShareDto;
import com.ogms.dge.container.modules.fs.entity.dto.SessionShareDto;
import com.ogms.dge.container.modules.fs.entity.enums.PageSize;
import com.ogms.dge.container.modules.fs.entity.enums.ResponseCodeEnum;
import com.ogms.dge.container.modules.fs.entity.enums.ShareValidTypeEnums;
import com.ogms.dge.container.modules.fs.entity.query.FileShareQuery;
import com.ogms.dge.container.modules.fs.entity.vo.PaginationResultVO;
import com.ogms.dge.container.modules.fs.mapper.FileShareMapper;
import com.ogms.dge.container.modules.fs.service.IFileShareService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 分享信息 服务实现类
 * </p>
 *
 * @author jin wang
 * @since 2023-09-07
 */
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements IFileShareService {

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    /**
     * 加载共享列表
     * @param query
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaginationResultVO findListByPage(FileShareQuery query) {
        LambdaQueryWrapper<FileShare> lqw = new LambdaQueryWrapper<>();
        // 默认每页显示15条数据
        int pageSize = query.getPageSize() == null? PageSize.SIZE15.getSize(): query.getPageSize();
        int pageNo = query.getPageNo() == null? 1: query.getPageNo();

        IPage page = new Page(pageNo, pageSize);

        lqw.eq(FileShare::getUserId, query.getUserId()).
                 orderByDesc(FileShare::getShareTime);
        this.baseMapper.selectPage(page, lqw);

        List<FileShare> shareList = page.getRecords();

        if(shareList.size()==0){
            return new PaginationResultVO<>();
        }

        // 替代联合查询
        // 获取 share_info 表的文件id
        List<String> fileIds = new ArrayList<>();
        for(FileShare share: shareList){
            fileIds.add(share.getFileId());
        }
        Long userId = shareList.get(0).getUserId();
        Map<String, FileShare> shareMap = shareList.stream().collect(
                Collectors.toMap( FileShare::getFileId, Function.identity(), (file1, file2) -> file2 ));

        // 根据文件id 从file_info表中查询文件信息
        LambdaQueryWrapper<FileInfo> lqw2 = new LambdaQueryWrapper<>();
        lqw2.eq(FileInfo::getUserId, userId)
                .in(FileInfo::getFileId, fileIds);
        List<FileInfo> fileInfoList = fileInfoService.getBaseMapper().selectList(lqw2);
        Map<String, FileInfo> fileMap = fileInfoList.stream().collect(
                Collectors.toMap( FileInfo::getFileId, Function.identity(), (file1, file2) -> file2 ));

        // 合并数据
        List<FileShareDto> fileShareDto = new ArrayList<>();

        for (String fileId : shareMap.keySet()) {
//            FileShareDto dto = new FileShareDto();
            FileShare _share = shareMap.get(fileId);
            FileInfo _file = fileMap.get(fileId);


            FileShareDto dto = CopyTools.copy(_share, FileShareDto.class);

//            dto.setFileId(fileId);
//            dto.setShareId(_share.getShareId());
//            dto.setUserId(_share.getUserId());
//            dto.setValidType(_share.getValidType());
//            dto.setExpireTime(_share.getExpireTime());
//            dto.setShareTime(_share.getShareTime());
//            dto.setCode(_share.getCode());
//            dto.setShowCount(_share.getShowCount());

            dto.setFileName(_file.getFileName());
            dto.setFolderType(_file.getFolderType());
            dto.setFileType(_file.getFileType());

            fileShareDto.add(dto);
        }

        PaginationResultVO<FileShareDto> paginationResultVO  = new PaginationResultVO<>();
        paginationResultVO.setTotalCount(page.getTotal());
        paginationResultVO.setPageSize(pageSize);
        paginationResultVO.setPageNo(pageNo);
        paginationResultVO.setPageTotal(page.getPages());
        paginationResultVO.setList(fileShareDto);

        return paginationResultVO;
    }

    /**
     * 保存共享文件信息
     * @param share
     */
    @Override
    public void saveShare(FileShare share) {
        ShareValidTypeEnums typeEnum = ShareValidTypeEnums.getByType(share.getValidType());
        if (null == typeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (typeEnum != ShareValidTypeEnums.FOREVER) {
            share.setExpireTime(DateUtil.getAfterDate(typeEnum.getDays()));
        }
        Date curDate = new Date();
        share.setShareTime(curDate);
        if (StringTools.isEmpty(share.getCode())) {
            share.setCode(StringTools.getRandomString(Constant.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomString(Constant.LENGTH_20));
        this.baseMapper.insert(share);
    }

    /**
     * 删除共享文件信息
     * @param shareIdArray
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] shareIdArray, Long userId) {
        LambdaQueryWrapper<FileShare> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileShare::getUserId, userId)
                .in(FileShare::getShareId, shareIdArray);
        Integer count = this.baseMapper.delete(lqw);

        if (count != shareIdArray.length) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    /**
     * 根据 分享id 获取分享信息
     * @param shareId
     * @return
     */
    @Override
    public FileShare getFileShareByShareId(String shareId) {
        LambdaQueryWrapper<FileShare> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileShare::getShareId, shareId);
        return  this.baseMapper.selectOne(lqw);
    }

    /**
     * 检查分享码
     * @param shareId
     * @param code
     * @return
     */
    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        // 查看有没有分享信息
        FileShare share = this.baseMapper.selectByShareId(shareId);
        // 分享有没有过期？
        boolean isExpire = (share.getExpireTime() != null && new Date().after(share.getExpireTime()));
        if (null == share || isExpire) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        // 判断输入的验证码对不对
        if (!share.getCode().equals(code)) {
            throw new BusinessException("提取码错误");
        }

        // 更新浏览次数
        this.baseMapper.updateShareShowCount(shareId);
        SessionShareDto shareSessionDto = new SessionShareDto();
        shareSessionDto.setShareId(shareId);
        shareSessionDto.setShareUserId(share.getUserId());
        shareSessionDto.setFileId(share.getFileId());
        shareSessionDto.setExpireTime(share.getExpireTime());
        return shareSessionDto;
    }
}
