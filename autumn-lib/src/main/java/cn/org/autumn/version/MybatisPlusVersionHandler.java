package cn.org.autumn.version;

import cn.org.autumn.config.VersionHandler;
import com.baomidou.mybatisplus.core.MybatisPlusVersion;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 版本
 */
@Order(4)
@Component
public class MybatisPlusVersionHandler implements VersionHandler {

    @Override
    public String name() {
        return "MyBatis-Plus";
    }

    @Override
    public String version() {
        try {
            return MybatisPlusVersion.getVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
