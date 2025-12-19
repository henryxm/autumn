package cn.org.autumn.exception;

/**
 * 自定义异常
 */
public class CodeException extends Exception {
    private static final long serialVersionUID = 1L;

    private String msg;

    private int code = 1;

    public CodeException() {
        super();
    }

    public CodeException(Throwable cause) {
        super(cause);
    }

    public CodeException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public CodeException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public CodeException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public CodeException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
