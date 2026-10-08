package cn.org.autumn.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseException extends CodeException implements ResponseThrowable {

    /** 业务失败默认码（勿与 {@link cn.org.autumn.model.Error#UNKNOWN_ERROR} 100000 混淆） */
    public static final int DEFAULT_CODE = 100001;

    public ResponseException() {
        super();
        setCode(DEFAULT_CODE);
    }

    public ResponseException(String message) {
        super(message, DEFAULT_CODE);
    }

    public ResponseException(String message, Throwable cause) {
        super(message, DEFAULT_CODE, cause);
    }

    public ResponseException(Throwable cause) {
        super(cause);
        setCode(DEFAULT_CODE);
    }

    public ResponseException(int code, String message) {
        super(message, code);
    }
}
