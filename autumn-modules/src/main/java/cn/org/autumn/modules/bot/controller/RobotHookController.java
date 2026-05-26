package cn.org.autumn.modules.bot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.bot.controller.gen.RobotHookControllerGen;

@RestController
@RequestMapping("bot/robothook")
public class RobotHookController extends RobotHookControllerGen {

}
