package cn.org.autumn.modules.bot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.bot.controller.gen.RobotControllerGen;

@RestController
@RequestMapping("bot/robot")
public class RobotController extends RobotControllerGen {

}
