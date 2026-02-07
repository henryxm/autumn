package cn.org.autumn.modules.db.controller;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.db.entity.DatabaseBackupEntity;
import cn.org.autumn.modules.db.entity.DatabaseBackupStrategyEntity;
import cn.org.autumn.modules.db.entity.DatabaseBackupUploadEntity;
import cn.org.autumn.modules.db.service.DatabaseBackupService;
import cn.org.autumn.modules.db.service.DatabaseBackupStrategyService;
import cn.org.autumn.modules.db.service.DatabaseBackupUploadService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/db/backup")
@Endpoint(hidden = true)
@SkipInterceptor
public class DatabaseController {

    @Autowired
    private DatabaseBackupService databaseBackupService;

    @Autowired
    private DatabaseBackupStrategyService databaseBackupStrategyService;

    @Autowired
    private DatabaseBackupUploadService databaseBackupUploadService;

    // ========================================
    // 页面
    // ========================================

    @GetMapping("/")
    public String index() {
        return "database";
    }

    // ========================================
    // 备份记录接口
    // ========================================

    @GetMapping("/list")
    @ResponseBody
    public R list(@RequestParam Map<String, Object> params) {
        try {
            PageUtils page = databaseBackupService.queryPage(params);
            return R.ok().put("page", page);
        } catch (Exception e) {
            log.error("获取备份列表失败:{}", e.getMessage());
            return R.error("获取备份列表失败: " + e.getMessage());
        }
    }

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
            log.error("获取备份详情失败:{}", e.getMessage());
            return R.error("获取备份详情失败: " + e.getMessage());
        }
    }

    /**
     * 执行备份（支持全量/指定表）
     */
    @PostMapping("/execute")
    @ResponseBody
    public R execute(@RequestBody(required = false) Map<String, Object> params) {
        try {
            String remark = "";
            String mode = "FULL";
            String tables = null;
            if (params != null) {
                if (params.get("remark") != null) {
                    remark = params.get("remark").toString();
                }
                if (params.get("mode") != null) {
                    mode = params.get("mode").toString();
                }
                if (params.get("tables") != null) {
                    tables = params.get("tables").toString();
                }
            }
            DatabaseBackupEntity entity = databaseBackupService.backupAsync(remark, mode, tables, null);
            return R.ok("备份任务已提交").put("info", entity);
        } catch (Exception e) {
            log.error("执行备份失败:{}", e.getMessage());
            return R.error("执行备份失败: " + e.getMessage());
        }
    }

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
            log.error("下载备份文件失败:{}", e.getMessage());
            try {
                response.sendError(500, "下载失败: " + e.getMessage());
            } catch (IOException ex) {
                log.error("发送错误响应失败:{}", ex.getMessage());
            }
        }
    }

    /**
     * 切换永久存储状态
     */
    @PostMapping("/togglePermanent/{id}")
    @ResponseBody
    public R togglePermanent(@PathVariable("id") Long id) {
        try {
            boolean success = databaseBackupService.togglePermanent(id);
            if (success) {
                DatabaseBackupEntity entity = databaseBackupService.selectById(id);
                return R.ok(Boolean.TRUE.equals(entity.getPermanent()) ? "已设为永久存储" : "已取消永久存储")
                        .put("permanent", entity.getPermanent());
            } else {
                return R.error("备份记录不存在");
            }
        } catch (Exception e) {
            log.error("切换永久存储失败:{}", e.getMessage());
            return R.error("操作失败: " + e.getMessage());
        }
    }

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
            log.error("更新备注失败:{}", e.getMessage());
            return R.error("更新备注失败: " + e.getMessage());
        }
    }

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
            log.error("删除备份失败:{}", e.getMessage());
            return R.error("删除备份失败: " + e.getMessage());
        }
    }

    @PostMapping("/deleteBatch")
    @ResponseBody
    public R deleteBatch(@RequestBody Long[] ids) {
        try {
            databaseBackupService.deleteBatch(ids);
            return R.ok("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除失败:{}", e.getMessage());
            return R.error("批量删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @ResponseBody
    public R stats() {
        try {
            Map<String, Object> stats = databaseBackupService.getStatistics();
            return R.ok().put("stats", stats);
        } catch (Exception e) {
            log.error("获取统计信息失败:{}", e.getMessage());
            return R.error("获取统计信息失败: " + e.getMessage());
        }
    }

    // ========================================
    // 任务控制接口（暂停/恢复/停止/进度）
    // ========================================

    /**
     * 暂停备份任务
     */
    @PostMapping("/task/pause/{id}")
    @ResponseBody
    public R pauseTask(@PathVariable("id") Long id) {
        try {
            boolean success = databaseBackupService.pauseTask(id);
            return success ? R.ok("任务已暂停") : R.error("任务不存在或已结束");
        } catch (Exception e) {
            log.error("暂停任务失败:{}", e.getMessage());
            return R.error("暂停任务失败: " + e.getMessage());
        }
    }

    /**
     * 恢复备份任务
     */
    @PostMapping("/task/resume/{id}")
    @ResponseBody
    public R resumeTask(@PathVariable("id") Long id) {
        try {
            boolean success = databaseBackupService.resumeTask(id);
            return success ? R.ok("任务已恢复") : R.error("任务不存在或已结束");
        } catch (Exception e) {
            log.error("恢复任务失败:{}", e.getMessage());
            return R.error("恢复任务失败: " + e.getMessage());
        }
    }

    /**
     * 停止备份任务
     */
    @PostMapping("/task/stop/{id}")
    @ResponseBody
    public R stopTask(@PathVariable("id") Long id) {
        try {
            boolean success = databaseBackupService.stopTask(id);
            return success ? R.ok("任务已停止") : R.error("任务不存在或已结束");
        } catch (Exception e) {
            log.error("停止任务失败:{}", e.getMessage());
            return R.error("停止任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有运行中的任务
     */
    @GetMapping("/task/running")
    @ResponseBody
    public R runningTasks() {
        try {
            return R.ok().put("tasks", databaseBackupService.getRunningTasks());
        } catch (Exception e) {
            log.error("获取运行中任务失败:{}", e.getMessage());
            return R.error("获取运行中任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个任务进度
     */
    @GetMapping("/task/progress/{id}")
    @ResponseBody
    public R taskProgress(@PathVariable("id") Long id) {
        try {
            Map<String, Object> progress = databaseBackupService.getTaskProgress(id);
            if (progress == null) {
                return R.error("任务不存在");
            }
            return R.ok().put("progress", progress);
        } catch (Exception e) {
            log.error("获取任务进度失败:{}", e.getMessage());
            return R.error("获取任务进度失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据库所有表名
     */
    @GetMapping("/tables")
    @ResponseBody
    public R databaseTables() {
        try {
            List<String> tables = databaseBackupService.getDatabaseTables();
            return R.ok().put("tables", tables);
        } catch (Exception e) {
            log.error("获取表列表失败:{}", e.getMessage());
            return R.error("获取表列表失败: " + e.getMessage());
        }
    }

    // ========================================
    // 策略管理接口
    // ========================================

    @GetMapping("/strategy/list")
    @ResponseBody
    public R strategyList(@RequestParam Map<String, Object> params) {
        try {
            PageUtils page = databaseBackupStrategyService.queryPage(params);
            return R.ok().put("page", page);
        } catch (Exception e) {
            log.error("获取策略列表失败:{}", e.getMessage());
            return R.error("获取策略列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/strategy/info/{id}")
    @ResponseBody
    public R strategyInfo(@PathVariable("id") Long id) {
        try {
            DatabaseBackupStrategyEntity entity = databaseBackupStrategyService.selectById(id);
            if (entity == null) {
                return R.error("策略不存在");
            }
            return R.ok().put("info", entity);
        } catch (Exception e) {
            log.error("获取策略详情失败:{}", e.getMessage());
            return R.error("获取策略详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/strategy/save")
    @ResponseBody
    public R strategySave(@RequestBody DatabaseBackupStrategyEntity entity) {
        try {
            if (entity.getId() == null) {
                entity.setCreateTime(new Date());
                databaseBackupStrategyService.insert(entity);
            } else {
                databaseBackupStrategyService.updateById(entity);
            }
            return R.ok("保存成功");
        } catch (Exception e) {
            log.error("保存策略失败:{}", e.getMessage());
            return R.error("保存策略失败: " + e.getMessage());
        }
    }

    @PostMapping("/strategy/delete/{id}")
    @ResponseBody
    public R strategyDelete(@PathVariable("id") Long id) {
        try {
            boolean success = databaseBackupStrategyService.deleteById(id);
            return success ? R.ok("删除成功") : R.error("策略不存在");
        } catch (Exception e) {
            log.error("删除策略失败:{}", e.getMessage());
            return R.error("删除策略失败: " + e.getMessage());
        }
    }

    /**
     * 手动执行策略
     */
    @PostMapping("/strategy/execute/{id}")
    @ResponseBody
    public R strategyExecute(@PathVariable("id") Long id) {
        try {
            databaseBackupStrategyService.executeStrategy(id);
            return R.ok("策略执行已提交");
        } catch (Exception e) {
            log.error("执行策略失败:{}", e.getMessage());
            return R.error("执行策略失败: " + e.getMessage());
        }
    }

    // ========================================
    // 上传管理接口
    // ========================================

    /**
     * 上传SQL备份文件（小文件直传，兼容保留）
     */
    @PostMapping("/upload")
    @ResponseBody
    public R upload(@RequestParam("file") MultipartFile file,
                    @RequestParam(value = "remark", required = false) String remark) {
        try {
            if (file.isEmpty()) {
                return R.error("请选择要上传的文件");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".sql")) {
                return R.error("只支持上传.sql格式的文件");
            }
            Map<String, Object> stats = databaseBackupService.getStatistics();
            String dbName = stats.get("database") != null ? stats.get("database").toString() : "";

            DatabaseBackupUploadEntity entity = databaseBackupUploadService.uploadFile(file, remark, dbName);
            return R.ok("上传成功").put("info", entity);
        } catch (Exception e) {
            log.error("上传备份文件失败:{}", e.getMessage());
            return R.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传 - 初始化
     */
    @PostMapping("/upload/chunk/init")
    @ResponseBody
    public R chunkUploadInit(@RequestBody Map<String, Object> params) {
        try {
            String filename = params.get("filename") != null ? params.get("filename").toString() : null;
            if (filename == null || !filename.toLowerCase().endsWith(".sql")) {
                return R.error("只支持上传.sql格式的文件");
            }
            long totalSize = Long.parseLong(params.get("totalSize").toString());
            int totalChunks = Integer.parseInt(params.get("totalChunks").toString());
            String remark = params.get("remark") != null ? params.get("remark").toString() : "";

            Map<String, Object> stats = databaseBackupService.getStatistics();
            String dbName = stats.get("database") != null ? stats.get("database").toString() : "";

            Map<String, Object> result = databaseBackupUploadService.initChunkUpload(filename, totalSize, totalChunks, remark, dbName);
            return R.ok("分片上传已初始化").put("data", result);
        } catch (Exception e) {
            log.error("初始化分片上传失败:{}", e.getMessage());
            return R.error("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传 - 上传单个分片
     */
    @PostMapping("/upload/chunk/upload")
    @ResponseBody
    public R chunkUpload(@RequestParam("uploadToken") String uploadToken,
                         @RequestParam("chunkIndex") int chunkIndex,
                         @RequestParam("chunk") MultipartFile chunk) {
        try {
            if (chunk.isEmpty()) {
                return R.error("分片数据为空");
            }
            Map<String, Object> result = databaseBackupUploadService.uploadChunk(uploadToken, chunkIndex, chunk);
            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("上传分片失败: token={}, chunk={}, error={}", uploadToken, chunkIndex, e.getMessage());
            return R.error("分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传 - 合并
     */
    @PostMapping("/upload/chunk/merge")
    @ResponseBody
    public R chunkMerge(@RequestBody Map<String, Object> params) {
        try {
            String uploadToken = params.get("uploadToken").toString();
            DatabaseBackupUploadEntity entity = databaseBackupUploadService.mergeChunks(uploadToken);
            return R.ok("上传成功").put("info", entity);
        } catch (Exception e) {
            log.error("合并分片失败:{}", e.getMessage());
            return R.error("合并失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传 - 取消
     */
    @PostMapping("/upload/chunk/cancel")
    @ResponseBody
    public R chunkCancel(@RequestBody Map<String, Object> params) {
        try {
            String uploadToken = params.get("uploadToken").toString();
            databaseBackupUploadService.cancelChunkUpload(uploadToken);
            return R.ok("已取消");
        } catch (Exception e) {
            log.error("取消分片上传失败:{}", e.getMessage());
            return R.error("取消失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件列表
     */
    @GetMapping("/upload/list")
    @ResponseBody
    public R uploadList(@RequestParam Map<String, Object> params) {
        try {
            PageUtils page = databaseBackupUploadService.queryPage(params);
            return R.ok().put("page", page);
        } catch (Exception e) {
            log.error("获取上传列表失败:{}", e.getMessage());
            return R.error("获取上传列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除上传的备份
     */
    @PostMapping("/upload/delete/{id}")
    @ResponseBody
    public R uploadDelete(@PathVariable("id") Long id) {
        try {
            boolean success = databaseBackupUploadService.deleteUpload(id);
            if (success) {
                return R.ok("删除成功");
            } else {
                return R.error("记录不存在或正在恢复中");
            }
        } catch (Exception e) {
            log.error("删除上传备份失败:{}", e.getMessage());
            return R.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 下载上传的备份文件
     */
    @GetMapping("/upload/download/{id}")
    public void uploadDownload(@PathVariable("id") Long id, HttpServletResponse response) {
        try {
            DatabaseBackupUploadEntity entity = databaseBackupUploadService.selectById(id);
            if (entity == null) {
                response.sendError(404, "记录不存在");
                return;
            }
            File file = databaseBackupUploadService.getUploadFile(id);
            if (file == null) {
                response.sendError(404, "文件不存在");
                return;
            }
            response.setContentType("application/octet-stream");
            String downloadName = entity.getOriginalFilename() != null ? entity.getOriginalFilename() : entity.getFilename();
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(downloadName, "UTF-8"));
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
            log.error("下载上传备份文件失败:{}", e.getMessage());
            try {
                response.sendError(500, "下载失败: " + e.getMessage());
            } catch (IOException ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    // ========================================
    // 恢复接口
    // ========================================

    /**
     * 从已有备份恢复
     */
    @PostMapping("/restore/{id}")
    @ResponseBody
    public R restoreFromBackup(@PathVariable("id") Long id) {
        try {
            Map<String, Object> result = databaseBackupService.restoreFromBackup(id);
            return R.ok("恢复任务已提交").put("restore", result);
        } catch (Exception e) {
            log.error("恢复备份失败:{}", e.getMessage());
            return R.error("恢复失败: " + e.getMessage());
        }
    }

    /**
     * 从上传的SQL文件恢复
     */
    @PostMapping("/upload/restore/{id}")
    @ResponseBody
    public R restoreFromUpload(@PathVariable("id") Long id) {
        try {
            Map<String, Object> result = databaseBackupService.restoreFromUpload(id);
            return R.ok("恢复任务已提交").put("restore", result);
        } catch (Exception e) {
            log.error("恢复上传备份失败:{}", e.getMessage());
            return R.error("恢复失败: " + e.getMessage());
        }
    }

    /**
     * 获取恢复任务进度
     */
    @GetMapping("/restore/progress/{taskKey}")
    @ResponseBody
    public R restoreProgress(@PathVariable("taskKey") String taskKey) {
        try {
            Map<String, Object> progress = databaseBackupService.getRestoreProgress(taskKey);
            return R.ok().put("progress", progress);
        } catch (Exception e) {
            log.error("获取恢复进度失败:{}", e.getMessage());
            return R.error("获取恢复进度失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有运行中的恢复任务
     */
    @GetMapping("/restore/running")
    @ResponseBody
    public R runningRestoreTasks() {
        try {
            return R.ok().put("tasks", databaseBackupService.getRunningRestoreTasks());
        } catch (Exception e) {
            log.error("获取运行中恢复任务失败:{}", e.getMessage());
            return R.error("获取运行中恢复任务失败: " + e.getMessage());
        }
    }
}
