package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(UserTokenHandler.class)
public interface UserTokenHandler {
    //为了提升执行效率，各个业务系统自己的Token应提交前缀，通过前缀来判断token是否是自己创建的
    //因为系统默认的Token是32位的UUID，因此，为了不冲突，业务系统务必使用前缀来标识一个Token
    boolean support(String token);

    //通过Token来获取用户的UUID
    String getUser(String token);
}