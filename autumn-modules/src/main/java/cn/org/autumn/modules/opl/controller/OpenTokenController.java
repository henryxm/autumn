package cn.org.autumn.modules.opl.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.opl.controller.gen.OpenTokenControllerGen;

@RestController
@RequestMapping("opl/opentoken")
public class OpenTokenController extends OpenTokenControllerGen {

}
