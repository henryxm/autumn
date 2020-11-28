package cn.org.autumn.modules.oauth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.oauth.controller.gen.ClientDetailsControllerGen;

/**
 * 客户端详情
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@RestController
@RequestMapping("oauth/clientdetails")
public class ClientDetailsController extends ClientDetailsControllerGen {

}
