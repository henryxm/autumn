package cn.org.autumn.exception;

import cn.org.autumn.model.Error;
import lombok.Getter;
import lombok.Setter;

/**
 * 自定义异常
 */
@Getter
@Setter
public class AException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String msg;

    private int code = 500;

    public AException() {
        super();
    }

    public AException(Throwable cause) {
        super(cause);
    }

    public AException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public AException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public AException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public AException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

    public AException(Error error) {
        super(error.getMsg());
        this.code = error.getCode();
        this.msg = error.getMsg();
    }
}
