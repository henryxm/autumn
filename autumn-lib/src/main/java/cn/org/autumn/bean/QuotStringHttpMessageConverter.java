package cn.org.autumn.bean;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.io.IOException;
import java.nio.charset.Charset;

public class QuotStringHttpMessageConverter extends StringHttpMessageConverter {

    public QuotStringHttpMessageConverter() {
        super();
    }

    public QuotStringHttpMessageConverter(Charset defaultCharset) {
        super(defaultCharset);
    }

    @Override
    protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
        String message = super.readInternal(clazz, inputMessage);
        if (message.startsWith("\"") && message.endsWith("\"")) {
            message = message.substring(1, message.length() - 1);
        }
        if (message.startsWith("'") && message.endsWith("'")) {
            message = message.substring(1, message.length() - 1);
        }
        return message;
    }
}