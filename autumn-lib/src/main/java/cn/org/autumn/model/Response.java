package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(name = "请求响应", description = "请求响应")
public class Response<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "返回数据", title = "响应数据", description = "响应数据", required = false)
    private T data;

    @Schema(name = "状态码", title = "响应代码", description = "响应代码", required = true)
    private int code;

    @Schema(name = "反馈信息", title = "错误信息", description = "错误信息:成功时为空或为success", required = false)
    private String msg;

    public boolean success() {
        return 0 == getCode();
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
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

    public static <T> Response<T> fail(T data, String msg) {
        Response<T> response = new Response<>();
        response.setCode(100000);
        response.setMsg(msg);
        response.setData(data);
        return response;
    }

    public static <T> Response<T> Null(String msg) {
        return fail(null, msg);
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