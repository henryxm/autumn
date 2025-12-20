package cn.org.autumn.model;

import cn.org.autumn.exception.AException;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.exception.ResponseThrowable;
import cn.org.autumn.search.IResult;
import cn.org.autumn.search.Result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "请求响应", description = "请求响应")
public class Response<T> extends DefaultEncrypt implements IResult {
    private static final long serialVersionUID = 1L;

    private Result result = new Result(Response.class);

    @Schema(name = "返回数据", title = "响应数据", description = "响应数据")
    private T data;

    @Schema(name = "状态码", title = "响应代码", description = "响应代码", required = true)
    private int code;

    @Schema(name = "反馈信息", title = "错误信息", description = "错误信息:成功时为空或为success")
    private String msg;

    public Response(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Response(Error error) {
        this.code = error.getCode();
        this.msg = error.getMsg();
    }

    public boolean success() {
        return 0 == getCode();
    }

    public boolean isSuccess() {
        return success();
    }

    /**
     * 获取对应的Error枚举
     *
     * @return Error枚举，如果未找到返回UNKNOWN_ERROR
     */
    public Error getError() {
        return Error.findByCode(code);
    }

    /**
     * 获取客户端操作建议
     *
     * @return 客户端操作类型
     */
    public Error.Action getAction() {
        return getError().getAction();
    }

    /**
     * 设置错误码和错误信息（使用Error枚举）
     *
     * @param error 错误枚举
     * @return 当前Response实例（支持链式调用）
     */
    public Response<T> setError(Error error) {
        this.code = error.getCode();
        this.msg = error.getMsg();
        return this;
    }

    public static Response<String> fail() {
        return Response.fail(Error.UNKNOWN_ERROR);
    }

    public static Response<String> fail(int code) {
        Error error = Error.findByCode(code);
        return Response.fail(error.getCode(), error.getMsg());
    }

    public static Response<String> fail(String msg) {
        return Response.fail(Error.UNKNOWN_ERROR.getCode(), msg);
    }

    public static <T> Response<T> error(String msg) {
        return fail(null, Error.UNKNOWN_ERROR.getCode(), msg);
    }

    public static <T> Response<T> error(int code, String msg) {
        return fail(null, code, msg);
    }

    public static <T> Response<T> error(CodeException exception) {
        return fail(null, exception.getCode(), exception.getMsg());
    }

    public static <T> Response<T> error(AException exception) {
        return fail(null, exception.getCode(), exception.getMsg());
    }

    /**
     * 使用Error枚举创建错误响应
     *
     * @param error 错误枚举
     * @param <T>   响应数据类型
     * @return 错误响应
     */
    public static <T> Response<T> error(Error error) {
        return fail(null, error.getCode(), error.getMsg());
    }

    /**
     * 使用Error枚举和数据创建错误响应
     *
     * @param data  响应数据
     * @param error 错误枚举
     * @param <T>   响应数据类型
     * @return 错误响应
     */
    public static <T> Response<T> error(T data, Error error) {
        return fail(data, error.getCode(), error.getMsg());
    }

    /**
     * 根据错误码创建错误响应
     *
     * @param code 错误码
     * @param <T>  响应数据类型
     * @return 错误响应，如果错误码不存在则使用UNKNOWN_ERROR
     */
    public static <T> Response<T> error(int code) {
        Error error = Error.findByCode(code);
        return fail(null, error.getCode(), error.getMsg());
    }

    public static <T> Response<T> fail(T data, String msg) {
        return fail(data, Error.UNKNOWN_ERROR.getCode(), msg);
    }

    /**
     * 使用Error枚举创建失败响应
     *
     * @param data  响应数据
     * @param error 错误枚举
     * @param <T>   响应数据类型
     * @return 失败响应
     */
    public static <T> Response<T> fail(T data, Error error) {
        return fail(data, error.getCode(), error.getMsg());
    }

    public static <T> Response<T> error(Throwable e) {
        return fail(null, e);
    }

    public static <T> Response<T> fail(T data, Throwable e) {
        if (log.isDebugEnabled() && null != e)
            log.debug("监控异常:", e);
        if (e instanceof ResponseThrowable) {
            ResponseThrowable exception = (ResponseThrowable) e;
            Response<T> response = new Response<>(exception.getCode(), exception.getMsg());
            response.setData(data);
            response.setResult(null);
            return response;
        } else {
            Response<T> response = new Response<>(Error.UNKNOWN_ERROR.getCode(), "您的访问出错啦，请稍后重试，谢谢！");
            response.setData(data);
            response.setResult(null);
            return response;
        }
    }

    public static <T> Response<T> fail(T data, int code, String msg) {
        Response<T> response = new Response<>(code, msg);
        response.setData(data);
        response.setResult(null);
        return response;
    }

    /**
     * 使用Error枚举创建失败响应（String类型）
     *
     * @param error 错误枚举
     * @return 失败响应
     */
    public static Response<String> fail(Error error) {
        return fail(error.getCode(), error.getMsg());
    }

    public static Response<String> fail(int code, String msg) {
        Response<String> response = new Response<>(code, msg);
        response.setData("fail");
        response.setResult(null);
        return response;
    }

    public static Response<String> ok() {
        return Response.ok("success");
    }

    public static Response<String> ok(String msg) {
        Response<String> response = new Response<>(0, msg);
        response.setData("success");
        return response;
    }

    public static <T> Response<T> ok(T data) {
        return Response.ok(data, "success");
    }

    public static <T> Response<T> ok(T data, String msg) {
        Response<T> response = new Response<>(0, msg);
        response.setData(data);
        return response;
    }
}