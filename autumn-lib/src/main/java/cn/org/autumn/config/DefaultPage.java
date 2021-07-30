package cn.org.autumn.config;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MAX_VALUE / 10)
public class DefaultPage implements PageHandler {

    @Override
    public String getOauth2Login() {
        return "oauth2/login";
    }

    @Override
    public String getLogin() {
        return "login";
    }

    @Override
    public String get404() {
        return "404";
    }

    @Override
    public String getError() {
        return "error";
    }

    @Override
    public String getHeader() {
        return "header";
    }

    @Override
    public String getIndex() {
        return "index";
    }

    @Override
    public String getMain() {
        return "main";
    }
}