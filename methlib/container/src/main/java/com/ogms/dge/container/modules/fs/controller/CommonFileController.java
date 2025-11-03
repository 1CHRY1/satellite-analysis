package com.ogms.dge.container.modules.fs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ogms.dge.container.common.exception.BusinessException;
import com.ogms.dge.container.common.utils.Constant;
import com.ogms.dge.container.common.utils.RedisComponent;
import com.ogms.dge.container.common.utils.StringTools;
import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.config.AppConfig;
import com.ogms.dge.container.modules.fs.entity.FileInfo;
import com.ogms.dge.container.modules.fs.entity.dto.DownloadFileDto;
import com.ogms.dge.container.modules.fs.entity.enums.FileFolderTypeEnums;
import com.ogms.dge.container.modules.fs.entity.enums.ResponseCodeEnum;
import com.ogms.dge.container.modules.fs.mapper.FileInfoMapper;
import com.ogms.dge.container.modules.fs.service.IFileInfoService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.List;

public class CommonFileController extends ABaseController{

    @Resource
    protected IFileInfoService fileInfoService;

    @Resource
    protected AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 获取文件夹信息
     * @param path
     * @param userId
     * @return
     */
    public R getFolderInfo(String path, Long userId) {

        if(path==null){
            throw new BusinessException("请求参数不合法");
        }

        String[] pathArray = path.split("/");

        LambdaQueryWrapper<FileInfo> lqw = new LambdaQueryWrapper<>();
        lqw.eq(FileInfo::getUserId, userId).
                        eq(FileInfo::getFolderType, FileFolderTypeEnums.FOLDER.getType()).
                        orderByAsc(FileInfo::getCreateTime).
                        in(FileInfo::getFileId, pathArray);

        List<FileInfo> fileInfoList = fileInfoService.getFolderInfo(lqw);

//        return R.success(fileInfoList);
        return R.ok().put("data", fileInfoList);
    }


    /**
     * 创建下载链接
     * @param fileId
     * @param userId
     * @return
     */
    protected R createDownloadUrl(String fileId, Long userId) {
        FileInfo fileInfo = ((FileInfoMapper)fileInfoService.getBaseMapper()).getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 不能下载文件夹
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 下载码
        String code = StringTools.getRandomString(Constant.LENGTH_50);
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        downloadFileDto.setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code, downloadFileDto);

//        return R.success(code);
        return R.ok().put("data", code);
    }


    /**
     * 下载
     * @param request
     * @param response
     * @param code
     * @throws Exception
     */
    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (null == downloadFileDto) {
            return;
        }
        String filePath = appConfig.getProjectFolder() + downloadFileDto.getFilePath();
        // String filePath = appConfig.getProjectFolder() + Constant.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        // IE浏览器
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }

}
