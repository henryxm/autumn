package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.search.IResult;
import cn.org.autumn.search.Result;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User implements IResult {

    private Result result = new Result(User.class);

    private String uuid;

    private String username;

    private String nickname;

    private String email;

    private String mobile;

    private String qq;

    private String weixin;

    private String alipay;

    private String idCard;

    private String icon;
}
