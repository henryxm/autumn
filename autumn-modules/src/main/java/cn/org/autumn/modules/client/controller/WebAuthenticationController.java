package cn.org.autumn.modules.client.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.client.controller.gen.WebAuthenticationControllerGen;

@RestController
@RequestMapping("client/webauthentication")
public class WebAuthenticationController extends WebAuthenticationControllerGen {

}
