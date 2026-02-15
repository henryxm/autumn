package cn.org.autumn.modules.db.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.db.entity.DatabaseBackupStrategyEntity;
import cn.org.autumn.modules.db.service.DatabaseBackupStrategyService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 备份策略
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
public class DatabaseBackupStrategyControllerGen {

    @Autowired
    protected DatabaseBackupStrategyService databaseBackupStrategyService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("db:databasebackupstrategy:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = databaseBackupStrategyService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("db:databasebackupstrategy:info")
    public R info(@PathVariable("id") Long id) {
        DatabaseBackupStrategyEntity databaseBackupStrategy = databaseBackupStrategyService.getById(id);
        return R.ok().put("databaseBackupStrategy" , databaseBackupStrategy);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("db:databasebackupstrategy:save")
    public R save(@RequestBody DatabaseBackupStrategyEntity databaseBackupStrategy) {
        databaseBackupStrategyService.save(databaseBackupStrategy);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("db:databasebackupstrategy:update")
    public R update(@RequestBody DatabaseBackupStrategyEntity databaseBackupStrategy) {
        ValidatorUtils.validateEntity(databaseBackupStrategy);
        databaseBackupStrategyService.updateById(databaseBackupStrategy);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("db:databasebackupstrategy:delete")
    public R delete(@RequestBody Long[] ids) {
        databaseBackupStrategyService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
