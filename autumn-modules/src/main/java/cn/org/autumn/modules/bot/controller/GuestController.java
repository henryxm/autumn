package cn.org.autumn.modules.bot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.bot.controller.gen.GuestControllerGen;

@RestController
@RequestMapping("bot/guest")
public class GuestController extends GuestControllerGen {

}
