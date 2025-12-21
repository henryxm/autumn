package cn.org.autumn.modules.usr.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.usr.controller.gen.UserProfileControllerGen;

/**
 * 用户信息
 *
 * @author User
 * @email henryxm@163.com
 * @date 2025-12
 */
@RestController
@RequestMapping("usr/userprofile")
public class UserProfileController extends UserProfileControllerGen {

}
