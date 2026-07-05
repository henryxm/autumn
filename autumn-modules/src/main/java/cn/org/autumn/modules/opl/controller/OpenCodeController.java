package cn.org.autumn.modules.opl.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.opl.controller.gen.OpenCodeControllerGen;

@RestController
@RequestMapping("opl/opencode")
public class OpenCodeController extends OpenCodeControllerGen {

}
