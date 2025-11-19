package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.annotation.SearchType;
import cn.org.autumn.search.IResult;
import cn.org.autumn.search.Result;
import lombok.*;
import org.apache.commons.beanutils.BeanUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SearchType(value = "User", name = "账号", alias = "用户", describe = "基础用户账号信息")
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

    private String openId;

    private String unionId;

    private String idCard;

    private String icon;

    private int status;

    private int verify;

    @SneakyThrows
    public User(SysUserEntity entity){
        BeanUtils.copyProperties(this,entity);
    }
}
