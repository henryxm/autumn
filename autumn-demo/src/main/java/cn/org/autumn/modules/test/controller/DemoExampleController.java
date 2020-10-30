package cn.org.autumn.modules.test.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.test.controller.gen.DemoExampleControllerGen;



/**
 * 测试例子
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
@RestController
@RequestMapping("test/demoexample")
public class DemoExampleController extends DemoExampleControllerGen{

}
