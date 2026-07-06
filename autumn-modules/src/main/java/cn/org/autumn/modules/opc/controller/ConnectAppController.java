package cn.org.autumn.modules.opc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.opc.controller.gen.ConnectAppControllerGen;

@RestController
@RequestMapping("opc/connectapp")
public class ConnectAppController extends ConnectAppControllerGen {

}
