package com.ogms.dge.workspace.modules.fs.controller;

import com.ogms.dge.workspace.common.utils.StringTools;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Slf4j
public class ABaseController {

//    /**
//     * 从session中获取已登录的用户信息
//     * @param session
//     * @return
//     */
//    protected SessionWebUserDto getUserInfoFromSession(HttpSession session) {
//       // SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_KEY);
//        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
//        sessionWebUserDto.setUserId("3178033358");
//        sessionWebUserDto.setUserName("admin");
//        String token = UUID.randomUUID().toString();
//        sessionWebUserDto.setToken(token);
//
//        return sessionWebUserDto;
//    }
//
//
//    /**
//     * 从session中获取分享信息
//     * @param session
//     * @param shareId
//     * @return
//     */
//    protected SessionShareDto getSessionShareFromSession(HttpSession session, String shareId) {
//        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constant.SESSION_SHARE_KEY + shareId);
//        return sessionShareDto;
//    }

    protected void readFile(HttpServletResponse response, String filePath) {
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("读取文件异常", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("IO异常", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("IO异常", e);
                }
            }
        }
    }
}
