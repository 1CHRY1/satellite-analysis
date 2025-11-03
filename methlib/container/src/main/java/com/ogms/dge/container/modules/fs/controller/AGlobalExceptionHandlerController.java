package com.ogms.dge.container.modules.fs.controller;


import com.ogms.dge.container.common.exception.BusinessException;

import com.ogms.dge.container.common.utils.R;
import com.ogms.dge.container.modules.fs.entity.enums.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

/**
 * 异常拦截器，当拦截到异常时，就会终止之前的请求处理方法，进而执行异常处理方法。
 */
@Slf4j
@RestControllerAdvice
public class AGlobalExceptionHandlerController extends ABaseController {

//    private static final Logger logger = LoggerFactory.getLogger(AGlobalExceptionHandlerController.class);

    @ExceptionHandler(value = Exception.class)
    Object handleException(Exception e, HttpServletRequest request) {
        log.error("请求错误，请求地址: {},错误信息:", request.getRequestURL(), e);
        R r = new R();
        // 404 
        if (e instanceof NoHandlerFoundException) {
            r.setCode(ResponseCodeEnum.CODE_404.getCode());
            r.setMsg(ResponseCodeEnum.CODE_404.getMsg());
        } else if (e instanceof BusinessException) {
            //业务错误
            BusinessException biz = (BusinessException) e;
            r.setCode(biz.getCode() == null ? ResponseCodeEnum.CODE_600.getCode() : biz.getCode());
            r.setMsg(biz.getMessage());
        } else if (e instanceof BindException || e instanceof MethodArgumentTypeMismatchException) {
            //参数类型错误
            r.setCode(ResponseCodeEnum.CODE_600.getCode());
            r.setMsg(ResponseCodeEnum.CODE_600.getMsg());
        } else if (e instanceof DuplicateKeyException) {
            //主键冲突
            r.setCode(ResponseCodeEnum.CODE_601.getCode());
            r.setMsg(ResponseCodeEnum.CODE_601.getMsg());
        } else {
            r.setCode(ResponseCodeEnum.CODE_500.getCode());
            r.setMsg(ResponseCodeEnum.CODE_500.getMsg());
        }
        return r;
    }
}
