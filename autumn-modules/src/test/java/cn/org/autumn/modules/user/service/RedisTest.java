package cn.org.autumn.modules.user.service;

import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.junit.Ignore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore("依赖本地Redis环境，默认构建环境跳过")
public class RedisTest {

    @Autowired
    private RedisUtils redisUtils;

    @Test
    public void contextLoads() {
        SysUserEntity user = new SysUserEntity();
        user.setEmail("123456@qq.com");
        redisUtils.set("user", user);
        System.out.println(ToStringBuilder.reflectionToString(redisUtils.get("user")));
    }

}
