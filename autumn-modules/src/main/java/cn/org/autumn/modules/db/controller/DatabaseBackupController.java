package cn.org.autumn.modules.db.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.db.controller.gen.DatabaseBackupControllerGen;

/**
 * 数据备份
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
@RestController
@RequestMapping("db/databasebackup")
public class DatabaseBackupController extends DatabaseBackupControllerGen {

}
