package cn.org.autumn.modules.opl.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.opl.controller.gen.OpenAppControllerGen;

@RestController
@RequestMapping("opl/openapp")
public class OpenAppController extends OpenAppControllerGen {

}
