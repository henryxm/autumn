package cn.org.autumn.modules.client.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.client.controller.gen.WebAuthenticationControllerGen;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@RestController
@RequestMapping("client/webauthentication")
public class WebAuthenticationController extends WebAuthenticationControllerGen {

}
