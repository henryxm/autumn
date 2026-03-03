package cn.org.autumn.model;

/**
 * 兼容响应包装：
 * 继承 Response，统一响应结构并显式表达“支持兼容加密返回”。
 * <p>
 * 约定：
 * 1) 新客户端携带 X-Encrypt-Session 时，可由响应拦截器对整个 CompatibleResponse 进行加密返回；
 * 2) 老客户端不携带加密头时，拦截器应降级返回 data 字段（即模板参数 T 对应的原始对象）。
 */
public class CompatibleResponse<T> extends Response<T> {
    private static final long serialVersionUID = 1L;

    public CompatibleResponse() {
        super();
    }

    public CompatibleResponse(int code, String msg) {
        super(code, msg);
    }

    public static <T> CompatibleResponse<T> ok(T data) {
        CompatibleResponse<T> response = new CompatibleResponse<>();
        response.setCode(0);
        response.setMsg("success");
        response.setData(data);
        return response;
    }

    /**
     * 兼容降级：
     * 老客户端不支持加密时，仅返回业务数据本体。
     */
    public T unwrap() {
        return getData();
    }
}
