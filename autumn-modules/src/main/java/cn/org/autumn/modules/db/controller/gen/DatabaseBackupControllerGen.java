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
import cn.org.autumn.modules.db.entity.DatabaseBackupEntity;
import cn.org.autumn.modules.db.service.DatabaseBackupService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 数据备份
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
public class DatabaseBackupControllerGen {

    @Autowired
    protected DatabaseBackupService databaseBackupService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("db:databasebackup:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = databaseBackupService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("db:databasebackup:info")
    public R info(@PathVariable("id") Long id) {
        DatabaseBackupEntity databaseBackup = databaseBackupService.selectById(id);
        return R.ok().put("databaseBackup" , databaseBackup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("db:databasebackup:save")
    public R save(@RequestBody DatabaseBackupEntity databaseBackup) {
        databaseBackupService.insert(databaseBackup);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("db:databasebackup:update")
    public R update(@RequestBody DatabaseBackupEntity databaseBackup) {
        ValidatorUtils.validateEntity(databaseBackup);
        databaseBackupService.updateAllColumnById(databaseBackup);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("db:databasebackup:delete")
    public R delete(@RequestBody Long[] ids) {
        databaseBackupService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
