package cn.org.autumn.modules.db.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.db.controller.gen.DatabaseBackupStrategyControllerGen;

/**
 * 备份策略
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
@RestController
@RequestMapping("db/databasebackupstrategy")
public class DatabaseBackupStrategyController extends DatabaseBackupStrategyControllerGen {

}
