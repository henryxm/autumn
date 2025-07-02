package cn.org.autumn.handler;

import cn.org.autumn.exception.AException;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.utils.R;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 异常处理器
 */
@RestControllerAdvice
public class AExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 处理自定义异常
     */
    @ExceptionHandler(AException.class)
    public R handleRRException(AException e) {
        if (logger.isDebugEnabled())
            logger.debug("AException:{}", new Gson().toJson(e));
        R r = new R();
        r.put("code", e.getCode());
        r.put("msg", e.getMessage());
        logger.debug("AException", e);
        return r;
    }

    @ExceptionHandler(CodeException.class)
    public R handleCodeException(CodeException e) {
        if (logger.isDebugEnabled())
            logger.debug("CodeException:{}", new Gson().toJson(e));
        R r = new R();
        r.put("code", e.getCode());
        r.put("msg", e.getMessage());
        return r;
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public R handleDuplicateKeyException(DuplicateKeyException e) {
        logger.debug("DuplicateKeyException", e);
        return R.error("数据库中已存在该记录");
    }

    @ExceptionHandler(Exception.class)
    public R handleException(Exception e) {
        //Suppress known exception log
        if (!(e instanceof HttpMessageNotReadableException) && !(e instanceof HttpRequestMethodNotSupportedException))
            logger.debug("Exception", e);
        return R.error(e.getMessage());
    }
}
