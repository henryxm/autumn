package cn.org.autumn.config;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Order(Integer.MAX_VALUE / 10)
public class DefaultPage implements PageHandler {

    @Override
    public String oauth2Login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "oauth2/login";
    }

    @Override
    public String login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "login";
    }

    @Override
    public String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return login(httpServletRequest, httpServletResponse, model);
    }

    @Override
    public String _404(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(404);
        return "404";
    }

    @Override
    public String error(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null != httpServletResponse)
            httpServletResponse.setStatus(500);
        return "error";
    }

    @Override
    public String header(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "header";
    }

    @Override
    public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "index";
    }

    @Override
    public String main(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        return "main";
    }
}