package cn.org.autumn.modules.lan.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.lan.controller.gen.LanguageControllerGen;



/**
 * 多语言
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
@RestController
@RequestMapping("lan/language")
public class LanguageController extends LanguageControllerGen{

}
