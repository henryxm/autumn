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
import cn.org.autumn.modules.db.entity.DatabaseBackupUploadEntity;
import cn.org.autumn.modules.db.service.DatabaseBackupUploadService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 备份上传
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
public class DatabaseBackupUploadControllerGen {

    @Autowired
    protected DatabaseBackupUploadService databaseBackupUploadService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("db:databasebackupupload:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = databaseBackupUploadService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("db:databasebackupupload:info")
    public R info(@PathVariable("id") Long id) {
        DatabaseBackupUploadEntity databaseBackupUpload = databaseBackupUploadService.getById(id);
        return R.ok().put("databaseBackupUpload" , databaseBackupUpload);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("db:databasebackupupload:save")
    public R save(@RequestBody DatabaseBackupUploadEntity databaseBackupUpload) {
        databaseBackupUploadService.save(databaseBackupUpload);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("db:databasebackupupload:update")
    public R update(@RequestBody DatabaseBackupUploadEntity databaseBackupUpload) {
        ValidatorUtils.validateEntity(databaseBackupUpload);
        databaseBackupUploadService.updateById(databaseBackupUpload);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("db:databasebackupupload:delete")
    public R delete(@RequestBody Long[] ids) {
        databaseBackupUploadService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
