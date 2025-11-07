package com.ogms.dge.workspace.modules.fs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.modules.fs.entity.FileInfo;
import com.ogms.dge.workspace.modules.fs.entity.enums.FileDelFlagEnums;
import com.ogms.dge.workspace.modules.fs.entity.enums.PageSize;
import com.ogms.dge.workspace.modules.fs.entity.vo.PaginationResultVO;
import com.ogms.dge.workspace.modules.fs.service.IFileInfoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/recycle")
public class RecycleController extends ABaseController{

    @Resource
    private IFileInfoService fileInfoService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadRecycleList")
//    @GlobalInterceptor(checkParams = true)
    public R loadRecycleList(/*HttpSession session,*/ Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();

        int pSize = pageNo == null? PageSize.SIZE15.getSize(): pageNo;
        int pNo = pageSize == null? 1: pageSize;

        IPage page = new Page(pNo, pSize);

        lqw.eq(FileInfo::getUserId, /*getUserId()*/1L).
                        eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag()).
                        orderByDesc(FileInfo::getRecoveryTime);
        this.fileInfoService.getBaseMapper().selectPage(page, lqw);

        PaginationResultVO<FileInfo> paginationResultVO  =new PaginationResultVO<>();
        paginationResultVO.setTotalCount(page.getTotal());
        paginationResultVO.setPageSize(pageSize);
        paginationResultVO.setPageNo(pageNo);
        paginationResultVO.setPageTotal(page.getPages());
        paginationResultVO.setList(page.getRecords());

        return R.ok().put("data", paginationResultVO);
    }

    /**
     * 从回收站删除文件
     * @param fileIds
     * @return
     */
    @PostMapping("/delFile")
//    @GlobalInterceptor(checkParams = true)
    public R delFile(/*HttpSession session,*/
                     /*@VerifyParam(required = true)*/  String fileIds) {
        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.delFileBatchFromRecycle(/*getUserId()*/1L, fileIds, false);
        return R.ok();
    }

    /**
     * 从回收站恢复文件
     * @param fileIds
     * @return
     */
    @RequestMapping("/recoverFile")
//    @GlobalInterceptor(checkParams = true)
    public R recoverFile(/*HttpSession session,*/
            /*@VerifyParam(required = true) */
                         String fileIds) {
        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.recoverFileBatchFromRecycle(/*getUserId()*/1L, fileIds);

        return R.ok();
    }

}
