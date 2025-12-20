package cn.org.autumn.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseException extends CodeException implements ResponseThrowable {

    //默认错误值
    int code = 100001;

    public ResponseException() {
        super();
    }

    public ResponseException(String message) {
        super(message);
    }

    public ResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResponseException(Throwable cause) {
        super(cause);
    }

    public ResponseException(int code, String message) {
        super(message);
        this.code = code;
    }
}
