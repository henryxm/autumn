package cn.org.autumn.modules.oauth.controller;

import cn.org.autumn.modules.oauth.controller.gen.TokenStoreControllerGen;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权令牌
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@RestController
@RequestMapping("oauth/tokenstore")
public class TokenStoreController extends TokenStoreControllerGen {

}
