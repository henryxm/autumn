package cn.org.autumn.modules.job.controller;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理控制器
 * <p>
 * 提供定时任务的查看、启停、手动触发、健康预警等管理功能
 */
@Slf4j
@Controller
@RequestMapping("/job/loop")
public class LoopJobController {

    /**
     * 管理页面
     */
    @GetMapping("")
    public String page() {
        return "loopjob";
    }

    /**
     * 获取全局统计信息
     */
    @GetMapping("/stats")
    @ResponseBody
    public R stats() {
        try {
            Map<String, Object> stats = LoopJob.getStats();
            return R.ok().put("stats", stats);
        } catch (Exception e) {
            log.error("获取统计信息失败:", e);
            return R.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类列表及统计（包含批量执行耗时数据）
     */
    @GetMapping("/categories")
    @ResponseBody
    public R categories() {
        try {
            List<Map<String, Object>> categories = LoopJob.getCategoryList();
            return R.ok().put("categories", categories);
        } catch (Exception e) {
            log.error("获取分类列表失败:", e);
            return R.error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务列表
     */
    @GetMapping("/list")
    @ResponseBody
    public R list(@RequestParam(required = false) String category) {
        try {
            List<Map<String, Object>> jobs = LoopJob.getJobList(category);
            return R.ok().put("jobs", jobs);
        } catch (Exception e) {
            log.error("获取任务列表失败:", e);
            return R.error("获取任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取健康预警列表
     */
    @GetMapping("/alerts")
    @ResponseBody
    public R alerts() {
        try {
            List<Map<String, Object>> alerts = LoopJob.getAlerts();
            return R.ok().put("alerts", alerts).put("count", alerts.size());
        } catch (Exception e) {
            log.error("获取告警信息失败:", e);
            return R.error("获取告警信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/enable")
    @ResponseBody
    public R enable(@RequestBody Map<String, Object> params) {
        try {
            String jobId = (String) params.get("jobId");
            if (jobId == null || jobId.isEmpty()) {
                return R.error("任务ID不能为空");
            }
            boolean result = LoopJob.enableJob(jobId);
            if (result) {
                return R.ok("任务已启用");
            } else {
                return R.error("未找到指定任务");
            }
        } catch (Exception e) {
            log.error("启用任务失败:", e);
            return R.error("启用任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/disable")
    @ResponseBody
    public R disable(@RequestBody Map<String, Object> params) {
        try {
            String jobId = (String) params.get("jobId");
            if (jobId == null || jobId.isEmpty()) {
                return R.error("任务ID不能为空");
            }
            boolean result = LoopJob.disableJob(jobId);
            if (result) {
                return R.ok("任务已禁用");
            } else {
                return R.error("未找到指定任务");
            }
        } catch (Exception e) {
            log.error("禁用任务失败:", e);
            return R.error("禁用任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/trigger")
    @ResponseBody
    public R trigger(@RequestBody Map<String, Object> params) {
        try {
            String jobId = (String) params.get("jobId");
            if (jobId == null || jobId.isEmpty()) {
                return R.error("任务ID不能为空");
            }
            boolean result = LoopJob.triggerJob(jobId);
            if (result) {
                return R.ok("任务已触发执行");
            } else {
                return R.error("未找到指定任务或任务正在执行中");
            }
        } catch (Exception e) {
            log.error("触发任务失败:", e);
            return R.error("触发任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/enableCategory")
    @ResponseBody
    public R enableCategory(@RequestBody Map<String, Object> params) {
        try {
            String category = (String) params.get("category");
            if (category == null || category.isEmpty()) {
                return R.error("分类名称不能为空");
            }
            LoopJob.enableCategory(category);
            return R.ok("分类已启用");
        } catch (Exception e) {
            log.error("启用分类失败:", e);
            return R.error("启用分类失败: " + e.getMessage());
        }
    }

    @PostMapping("/disableCategory")
    @ResponseBody
    public R disableCategory(@RequestBody Map<String, Object> params) {
        try {
            String category = (String) params.get("category");
            if (category == null || category.isEmpty()) {
                return R.error("分类名称不能为空");
            }
            LoopJob.disableCategory(category);
            return R.ok("分类已禁用");
        } catch (Exception e) {
            log.error("禁用分类失败:", e);
            return R.error("禁用分类失败: " + e.getMessage());
        }
    }

    @PostMapping("/pauseAll")
    @ResponseBody
    public R pauseAll() {
        try {
            LoopJob.pauseAll();
            return R.ok("所有定时任务已暂停");
        } catch (Exception e) {
            log.error("全局暂停失败:", e);
            return R.error("全局暂停失败: " + e.getMessage());
        }
    }

    @PostMapping("/resumeAll")
    @ResponseBody
    public R resumeAll() {
        try {
            LoopJob.resumeAll();
            return R.ok("所有定时任务已恢复");
        } catch (Exception e) {
            log.error("全局恢复失败:", e);
            return R.error("全局恢复失败: " + e.getMessage());
        }
    }

    /**
     * 动态更新任务运行时配置
     */
    @PostMapping("/updateConfig")
    @ResponseBody
    public R updateConfig(@RequestBody Map<String, Object> params) {
        try {
            String jobId = (String) params.get("jobId");
            if (jobId == null || jobId.isEmpty()) {
                return R.error("任务ID不能为空");
            }
            boolean result = LoopJob.updateJobConfig(jobId, params);
            if (result) {
                return R.ok("配置已更新");
            } else {
                return R.error("未找到指定任务");
            }
        } catch (Exception e) {
            log.error("更新配置失败:", e);
            return R.error("更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 重置指定任务的统计数据
     */
    @PostMapping("/resetStats")
    @ResponseBody
    public R resetStats(@RequestBody Map<String, Object> params) {
        try {
            String jobId = (String) params.get("jobId");
            if (jobId == null || jobId.isEmpty()) {
                return R.error("任务ID不能为空");
            }
            boolean result = LoopJob.resetJobStats(jobId);
            if (result) {
                return R.ok("统计数据已重置");
            } else {
                return R.error("未找到指定任务");
            }
        } catch (Exception e) {
            log.error("重置统计失败:", e);
            return R.error("重置统计失败: " + e.getMessage());
        }
    }

    /**
     * 切换并行执行模式
     */
    @PostMapping("/toggleParallel")
    @ResponseBody
    public R toggleParallel(@RequestBody Map<String, Object> params) {
        try {
            Boolean enabled = (Boolean) params.get("enabled");
            if (enabled == null) {
                // 无参数则切换
                enabled = !LoopJob.isParallelExecution();
            }
            LoopJob.setParallelExecution(enabled);
            return R.ok("并行执行已" + (enabled ? "启用" : "关闭"));
        } catch (Exception e) {
            log.error("切换并行执行失败:", e);
            return R.error("切换并行执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有任务的打印信息（兼容旧接口）
     */
    @GetMapping("/print")
    @ResponseBody
    public R print() {
        try {
            Map<String, java.util.List<String>> data = LoopJob.print();
            return R.ok().put("data", data);
        } catch (Exception e) {
            log.error("获取打印信息失败:", e);
            return R.error("获取打印信息失败: " + e.getMessage());
        }
    }
}
