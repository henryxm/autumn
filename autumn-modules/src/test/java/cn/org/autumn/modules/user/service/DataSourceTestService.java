package cn.org.autumn.modules.user.service;

import cn.org.autumn.datasources.DataSourceNames;
import cn.org.autumn.datasources.annotation.DataSource;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 测试多数据源
 */
@Service
public class DataSourceTestService {
    @Autowired
    private SysUserService sysUserService;

    public SysUserEntity queryUser(Long userId) {
        return sysUserService.getById(userId);
    }

    @DataSource(name = DataSourceNames.SECOND)
    public SysUserEntity queryUser2(Long userId) {
        return sysUserService.getById(userId);
    }
}
