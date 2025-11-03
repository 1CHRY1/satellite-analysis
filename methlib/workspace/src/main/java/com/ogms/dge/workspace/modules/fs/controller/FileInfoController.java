package com.ogms.dge.workspace.modules.fs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ogms.dge.workspace.common.utils.R;
import com.ogms.dge.workspace.common.utils.StringTools;
import com.ogms.dge.workspace.modules.fs.controller.CommonFileController;
import com.ogms.dge.workspace.modules.fs.entity.FileInfo;
import com.ogms.dge.workspace.modules.fs.entity.dto.UploadResultDto;
import com.ogms.dge.workspace.modules.fs.entity.enums.FileCategoryEnums;
import com.ogms.dge.workspace.modules.fs.entity.enums.FileDelFlagEnums;
import com.ogms.dge.workspace.modules.fs.entity.enums.FileFolderTypeEnums;
import com.ogms.dge.workspace.modules.fs.entity.query.FileInfoQuery;
import com.ogms.dge.workspace.modules.fs.entity.vo.PaginationResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * <p>
 * 文件信息 前端控制器
 * </p>
 *
 * @author jin wang
 * @since 2023-08-30
 */
@RestController
@RequestMapping("/file")
@Api(value = "Swagger测试接口", tags = {"Swagger测试接口"})
public class FileInfoController extends CommonFileController {
    @ApiOperation("加载列表")
    @PostMapping("/loadDataList")
//  @GlobalInterceptor(checkParams = true)
    public R loadDataList(/*HttpSession session,*/
            @RequestBody FileInfoQuery query) {
        /**
         * 第一次请求参数:
         * pageNo: undefined
         * pageSize: undefined
         * fileNameFuzzy: undefined  模糊查询
         * category: 'all'
         * filePid: '0'
         */
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(query.getCategory());
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(handleNullUserId());
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO<FileInfo> info = fileInfoService.findListByPage(query);

//        return R.success(info);
        return R.ok().put("data", info);
    }

    @ApiOperation("通过id加载")
    @RequestMapping("/loadData/{fileId}")
//  @GlobalInterceptor(checkParams = true)
    public R loadDataList(/*HttpSession session,*/
            @PathVariable("fileId") /*@VerifyParam(required = true)*/ String fileId) {
        FileInfo info = fileInfoService.getOne(new QueryWrapper<FileInfo>().eq("file_id", fileId));
        return R.ok().put("data", info);
    }

    @ApiOperation("通过 fileIdList 加载")
    @RequestMapping("/loadDataByIds")
    public R loadDataByIds(@RequestBody List<String> fileIdList) {
        // 使用 fileIdList 进行批量查询
        List<FileInfo> fileInfoList = fileInfoService.list(new QueryWrapper<FileInfo>().in("file_id", fileIdList));
        return R.ok().put("data", fileInfoList);
    }

    /**
     * 上传文件
     *
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid
     * @param fileMd5
     * @param chunkIndex
     * @param chunks
     * @return
     */
    @ApiOperation("上传文件")
    @PostMapping("/uploadFile")
    public R uploadFile(/*HttpSession session,*/
            String fileId,
            MultipartFile file,
            String fileName,
            String filePid,
            String fileMd5,
            Integer chunkIndex,
            Integer chunks) {

        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(handleNullUserId(), fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
//        return R.success(resultDto);
        return R.ok().put("data", resultDto);
    }

    /**
     * 移到文件到回收站
     *
     * @param fileIds
     * @return
     */
    @ApiOperation("移到文件到回收站")
    @PostMapping("/delFile")
//    @GlobalInterceptor(checkParams = true)
    public R delFile(/*HttpSession session,*/ /*@VerifyParam(required = true) */String fileIds) {
        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(handleNullUserId(), fileIds);
        return R.ok();
    }


    /**
     * 创建文件夹
     *
     * @param filePid
     * @param fileName
     * @return
     */
    @ApiOperation("创建文件夹")
    @PostMapping("/newFoloder")
//    @GlobalInterceptor(checkParams = true)
    public R newFoloder(/*HttpSession session,*/
            /*@VerifyParam(required = true)*/ String filePid,
            /* @VerifyParam(required = true)*/ String fileName) {
        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, handleNullUserId(), fileName);
        return R.ok().put("data", fileInfo);//getSuccessResponseVO(fileInfo);
    }


    /**
     * 获取文件夹信息
     *
     * @param path
     * @return
     */
    @ApiOperation("获取文件夹信息")
    @PostMapping("/getFolderInfo")
//    @GlobalInterceptor(checkParams = true)
    public R getFolderInfo(/*HttpSession session,*/
            /* @VerifyParam(required = true)*/ String path, String shareId) {
        return super.getFolderInfo(path, handleNullUserId());
    }

    @ApiOperation("重命名")
    @PostMapping("/rename")
//    @GlobalInterceptor(checkParams = true)
    public R rename(/*HttpSession session,*/
            /*  @VerifyParam(required = true) */String fileId,
            /*   @VerifyParam(required = true)*/ String fileName) {
        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, handleNullUserId(), fileName);
        return R.ok().put("data", fileInfo);
    }

    /**
     * 加载指定目录下的所有文件夹
     *
     * @param filePid
     * @param currentFileIds
     * @return
     */
    @ApiOperation("加载指定目录下的所有文件夹")
    @PostMapping("/loadAllFolder")
//    @GlobalInterceptor(checkParams = true)
    public R loadAllFolder(/*HttpSession session,*/
            /*@VerifyParam(required = true)*/ String filePid, String currentFileIds) {

        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileInfo::getUserId, handleNullUserId())
                .eq(FileInfo::getFilePid, filePid)
                .eq(FileInfo::getFolderType, FileFolderTypeEnums.FOLDER.getType())
                .eq(FileInfo::getDelFlag, FileDelFlagEnums.USING.getFlag())
                .orderByDesc(FileInfo::getCreateTime);
        if (!StringTools.isEmpty(currentFileIds)) {
            lqw.notIn(FileInfo::getFileId, currentFileIds.split(","));
        }

        List<FileInfo> fileInfoList = this.fileInfoService.getBaseMapper().selectList(lqw);

        return R.ok().put("data", fileInfoList);
    }

    /**
     * 改变文件夹
     *
     * @param fileIds
     * @param filePid
     * @return
     */
    @ApiOperation("改变文件夹")
    @PostMapping("/changeFileFolder")
//    @GlobalInterceptor(checkParams = true)
    public R changeFileFolder(/*HttpSession session,*/
            /* @VerifyParam(required = true) */ String fileIds,
            /* @VerifyParam(required = true)*/ String filePid) {
        // SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, handleNullUserId());

        return R.ok();
    }


    /**
     * 创建下载URL
     *
     * @param fileId
     * @return
     */
    @ApiOperation("创建下载URL")
    @PostMapping("/createDownloadUrl/{fileId}")
//    @GlobalInterceptor(checkParams = true)
    public R createDownloadUrl(/*HttpSession session,*/
            @PathVariable("fileId") /*@VerifyParam(required = true)*/ String fileId) {

        return super.createDownloadUrl(fileId, handleNullUserId());
    }


    /**
     * 下载文件
     *
     * @param request
     * @param response
     * @param code
     * @throws Exception
     */
    @Override
    @ApiOperation("下载文件")
    @RequestMapping("/download/{code}")
//    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code") /*@VerifyParam(required = true)*/ String code) throws Exception {
        super.download(request, response, code);
    }

    // 临时方法
    private Long handleNullUserId() {
        Long userId = 3L;
        try {
//            userId = getUserId();
            userId = 1L; // TODO: 做用户
        } catch (Exception e) {
            userId = 3L;
        } finally {
            return userId;
        }
    }

}
