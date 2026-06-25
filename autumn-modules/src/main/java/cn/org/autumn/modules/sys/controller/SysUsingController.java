package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.model.Using;
import cn.org.autumn.site.UsingFactory;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sys")
@Slf4j
public class SysUsingController {
    @Autowired
    UsingFactory usingFactory;

    @RequestMapping(value = "using", method = {RequestMethod.POST, RequestMethod.GET})
    public Using using(@RequestBody(required = false) String value, HttpServletRequest request) {
        String auth = request.getHeader("Authentication");
        if (log.isDebugEnabled())
            log.debug("Received using request: value={}, auth={}, method={}", value, auth, request.getMethod());

        // 处理value后面可能带的等号
        if (StringUtils.isNotBlank(value) && value.endsWith("=")) {
            value = value.substring(0, value.length() - 1);
            if (log.isDebugEnabled())
                log.debug("Value after removing equals sign: {}", value);
        }

        if (!Objects.equals(auth, UsingFactory.key) || StringUtils.isBlank(value))
            return new Using("参数错误");
        return new Using(usingFactory.using(value));
    }
}
