package cn.org.autumn.modules.opc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.opc.controller.gen.ConnectBindControllerGen;

@RestController
@RequestMapping("opc/connectbind")
public class ConnectBindController extends ConnectBindControllerGen {

}
