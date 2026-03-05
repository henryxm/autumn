package cn.org.autumn.modules.oauth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.oauth.controller.gen.SecurityRequestControllerGen;

/**
 * 安全验证
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-03
 */
@RestController
@RequestMapping("oauth/securityrequest")
public class SecurityRequestController extends SecurityRequestControllerGen {

}
