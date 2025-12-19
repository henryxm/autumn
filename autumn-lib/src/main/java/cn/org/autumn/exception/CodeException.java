package cn.org.autumn.exception;

import cn.org.autumn.model.Error;
import lombok.Getter;
import lombok.Setter;

/**
 * 自定义异常
 */
@Getter
@Setter
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

    public CodeException(Error error) {
        super(error.getMsg());
        this.code = error.getCode();
        this.msg = error.getMsg();
    }
}
