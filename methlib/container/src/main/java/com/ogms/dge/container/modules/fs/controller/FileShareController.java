package com.ogms.dge.container.modules.fs.controller;

import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.modules.fs.entity.FileShare;
import com.ogms.dge.container.modules.fs.entity.query.FileShareQuery;
import com.ogms.dge.container.modules.fs.entity.vo.PaginationResultVO;
import com.ogms.dge.container.modules.fs.service.IFileShareService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("shareController")
@RequestMapping("/share")
public class FileShareController extends ABaseController {
    @Resource
    private IFileShareService fileShareService;

    /**
     * 加载共享文件列表
     * @param query
     * @return
     */
    @RequestMapping("/loadShareList")
//    @GlobalInterceptor(checkParams = true)
    public R loadShareList(/*HttpSession session,*/
                           @RequestBody FileShareQuery query) {
        // SessionWebUserDto userDto = getUserInfoFromSession(session);
        query.setUserId(getUserId());
        PaginationResultVO resultVO = this.fileShareService.findListByPage(query);
        return R.ok().put("data", resultVO);
    }

    /**
     * 保存共享文件信息
     * @param fileId
     * @param validType
     * @param code
     * @return
     */
    @RequestMapping("/shareFile")
//    @GlobalInterceptor(checkParams = true)
    public R shareFile(/*HttpSession session,*/
                               /* @VerifyParam(required = true)*/ String fileId,
                              /*  @VerifyParam(required = true)*/ Integer validType,
                                String code) {
        // SessionWebUserDto userDto = getUserInfoFromSession(session);
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setValidType(validType);
        share.setCode(code);
        share.setUserId(getUserId());
        fileShareService.saveShare(share);
        return R.ok().put("data", share);
    }

    /**
     * 取消分享
     * @param shareIds
     * @return
     */
    @RequestMapping("/cancelShare")
//    @GlobalInterceptor(checkParams = true)
    public R cancelShare(/*HttpSession session,*/
                         /*@VerifyParam(required = true)*/ String shareIds) {
        // SessionWebUserDto userDto = getUserInfoFromSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), getUserId());
        return R.ok();
    }
}
