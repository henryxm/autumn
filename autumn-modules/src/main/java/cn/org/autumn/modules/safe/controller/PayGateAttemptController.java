package cn.org.autumn.modules.safe.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.safe.controller.gen.PayGateAttemptControllerGen;

@RestController
@RequestMapping("safe/paygateattempt")
public class PayGateAttemptController extends PayGateAttemptControllerGen {

}
