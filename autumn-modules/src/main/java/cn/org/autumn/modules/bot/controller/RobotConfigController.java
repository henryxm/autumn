package cn.org.autumn.modules.bot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.bot.controller.gen.RobotConfigControllerGen;

@RestController
@RequestMapping("bot/robotconfig")
public class RobotConfigController extends RobotConfigControllerGen {

}
