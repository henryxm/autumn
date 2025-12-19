package cn.org.autumn.model;

import cn.org.autumn.exception.AException;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.search.IResult;
import cn.org.autumn.search.Result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "请求响应", description = "请求响应")
public class Response<T> extends DefaultEncrypt implements IResult, Serializable {
    private static final long serialVersionUID = 1L;

    Result result = new Result(Response.class);

    @Schema(name = "返回数据", title = "响应数据", description = "响应数据", required = false)
    private T data;

    @Schema(name = "状态码", title = "响应代码", description = "响应代码", required = true)
    private int code;

    @Schema(name = "反馈信息", title = "错误信息", description = "错误信息:成功时为空或为success", required = false)
    private String msg;

    public boolean success() {
        return 0 == getCode();
    }

    public boolean isSuccess() {
        return success();
    }

    public static Response<String> fail() {
        return Response.fail(100000, "fail");
    }

    public static Response<String> fail(int code) {
        return Response.fail(code, "fail");
    }

    public static Response<String> fail(String msg) {
        return Response.fail(100000, msg);
    }

    public static <T> Response<T> error(String msg) {
        return fail(null, msg);
    }

    public static <T> Response<T> error(int code, String msg) {
        return fail(null, code, msg);
    }

    public static <T> Response<T> fail(T data, String msg) {
        Response<T> response = new Response<>();
        response.setCode(100000);
        response.setMsg(msg);
        response.setData(data);
        return response;
    }

    public static <T> Response<T> error(Throwable e) {
        return fail(null, e);
    }

    public static <T> Response<T> fail(T data, Throwable e) {
        if (log.isDebugEnabled() && null != e)
            log.debug("监控异常:", e);
        if (e instanceof AException) {
            AException exception = (AException) e;
            Response<T> response = new Response<>();
            response.setCode(exception.getCode());
            response.setMsg(exception.getMessage());
            response.setData(data);
            return response;
        } else if (e instanceof CodeException) {
            CodeException exception = (CodeException) e;
            Response<T> response = new Response<>();
            response.setCode(exception.getCode());
            response.setMsg(exception.getMessage());
            response.setData(data);
            return response;
        } else {
            Response<T> response = new Response<>();
            response.setCode(100000);
            response.setMsg("您的访问出错啦，请稍后重试，谢谢！");
            response.setData(data);
            return response;
        }
    }

    public static <T> Response<T> fail(T data, int code, String msg) {
        Response<T> response = new Response<>();
        response.setCode(code);
        response.setMsg(msg);
        response.setData(data);
        return response;
    }

    public static Response<String> fail(int code, String msg) {
        Response<String> response = new Response<>();
        response.setCode(code);
        response.setMsg(msg);
        response.setData("fail");
        return response;
    }

    public static Response<String> ok() {
        return Response.ok("success");
    }

    public static Response<String> ok(String msg) {
        Response<String> response = new Response<>();
        response.setCode(0);
        response.setMsg(msg);
        response.setData("success");
        return response;
    }

    public static <T> Response<T> ok(T data) {
        return Response.ok(data, "success");
    }

    public static <T> Response<T> ok(T data, String msg) {
        Response<T> response = new Response<>();
        response.setCode(0);
        response.setMsg(msg);
        response.setData(data);
        return response;
    }
}