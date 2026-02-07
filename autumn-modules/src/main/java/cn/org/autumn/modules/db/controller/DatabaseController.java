package cn.org.autumn.modules.db.controller;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.db.entity.DatabaseBackupEntity;
import cn.org.autumn.modules.db.service.DatabaseBackupService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/db/backup")
@Endpoint(hidden = true)
@SkipInterceptor
public class DatabaseController {

    @Autowired
    private DatabaseBackupService databaseBackupService;

    /**
     * 数据库备份管理页面
     */
    @GetMapping("/")
    public String index() {
        return "database";
    }

    /**
     * 备份列表（分页）
     */
    @GetMapping("/list")
    @ResponseBody
    public R list(@RequestParam Map<String, Object> params) {
        try {
            PageUtils page = databaseBackupService.queryPage(params);
            return R.ok().put("page", page);
        } catch (Exception e) {
            log.error("获取备份列表失败", e);
            return R.error("获取备份列表失败: " + e.getMessage());
        }
    }

    /**
     * 备份详情
     */
    @GetMapping("/info/{id}")
    @ResponseBody
    public R info(@PathVariable("id") Long id) {
        try {
            DatabaseBackupEntity entity = databaseBackupService.selectById(id);
            if (entity == null) {
                return R.error("备份记录不存在");
            }
            return R.ok().put("info", entity);
        } catch (Exception e) {
            log.error("获取备份详情失败", e);
            return R.error("获取备份详情失败: " + e.getMessage());
        }
    }

    /**
     * 执行备份
     */
    @PostMapping("/execute")
    @ResponseBody
    public R execute(@RequestBody(required = false) Map<String, Object> params) {
        try {
            String remark = "";
            if (params != null && params.get("remark") != null) {
                remark = params.get("remark").toString();
            }
            DatabaseBackupEntity entity = databaseBackupService.backup(remark);
            if (entity.getStatus() == 1) {
                return R.ok("备份成功").put("info", entity);
            } else {
                return R.error("备份失败: " + entity.getError());
            }
        } catch (Exception e) {
            log.error("执行备份失败", e);
            return R.error("执行备份失败: " + e.getMessage());
        }
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable("id") Long id, HttpServletResponse response) {
        try {
            DatabaseBackupEntity entity = databaseBackupService.selectById(id);
            if (entity == null) {
                response.sendError(404, "备份记录不存在");
                return;
            }
            File file = databaseBackupService.getBackupFile(id);
            if (file == null) {
                response.sendError(404, "备份文件不存在");
                return;
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(entity.getFilename(), "UTF-8"));
            response.setContentLengthLong(file.length());
            try (InputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        } catch (Exception e) {
            log.error("下载备份文件失败", e);
            try {
                response.sendError(500, "下载失败: " + e.getMessage());
            } catch (IOException ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 修改备注
     */
    @PostMapping("/remark")
    @ResponseBody
    public R updateRemark(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String remark = params.get("remark") != null ? params.get("remark").toString() : "";

            boolean success = databaseBackupService.updateRemark(id, remark);
            if (success) {
                return R.ok("备注已更新");
            } else {
                return R.error("备份记录不存在");
            }
        } catch (Exception e) {
            log.error("更新备注失败", e);
            return R.error("更新备注失败: " + e.getMessage());
        }
    }

    /**
     * 删除备份
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public R delete(@PathVariable("id") Long id) {
        try {
            boolean success = databaseBackupService.deleteBackup(id);
            if (success) {
                return R.ok("删除成功");
            } else {
                return R.error("备份记录不存在");
            }
        } catch (Exception e) {
            log.error("删除备份失败", e);
            return R.error("删除备份失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除
     */
    @PostMapping("/deleteBatch")
    @ResponseBody
    public R deleteBatch(@RequestBody Long[] ids) {
        try {
            databaseBackupService.deleteBatch(ids);
            return R.ok("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return R.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 统计信息
     */
    @GetMapping("/stats")
    @ResponseBody
    public R stats() {
        try {
            Map<String, Object> stats = databaseBackupService.getStatistics();
            return R.ok().put("stats", stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return R.error("获取统计信息失败: " + e.getMessage());
        }
    }
}
