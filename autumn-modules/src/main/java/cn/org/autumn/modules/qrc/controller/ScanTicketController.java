package cn.org.autumn.modules.qrc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.qrc.controller.gen.ScanTicketControllerGen;

@RestController
@RequestMapping("qrc/scanticket")
public class ScanTicketController extends ScanTicketControllerGen {

}
