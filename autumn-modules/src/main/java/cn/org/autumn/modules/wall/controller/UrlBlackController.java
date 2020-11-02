package cn.org.autumn.modules.wall.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.wall.controller.gen.UrlBlackControllerGen;

/**
 * 链接黑名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@RestController
@RequestMapping("wall/urlblack")
public class UrlBlackController extends UrlBlackControllerGen {

}
