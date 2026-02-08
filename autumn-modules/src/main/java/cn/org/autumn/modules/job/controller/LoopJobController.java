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
 * 提供定时任务的查看、启停、手动触发等管理功能
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
     * 获取分类列表及统计
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
     * 获取任务列表，可按分类过滤
     *
     * @param category 分类名称（可选），如 OneSecond、ThreeSecond 等
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
     * 启用指定任务
     *
     * @param params 包含 jobId 字段
     */
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

    /**
     * 禁用指定任务
     *
     * @param params 包含 jobId 字段
     */
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

    /**
     * 手动触发指定任务
     *
     * @param params 包含 jobId 字段
     */
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
                return R.error("未找到指定任务");
            }
        } catch (Exception e) {
            log.error("触发任务失败:", e);
            return R.error("触发任务失败: " + e.getMessage());
        }
    }

    /**
     * 启用指定分类的所有任务
     *
     * @param params 包含 category 字段
     */
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

    /**
     * 禁用指定分类的所有任务
     *
     * @param params 包含 category 字段
     */
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

    /**
     * 全局暂停所有定时任务
     */
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

    /**
     * 恢复所有定时任务
     */
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
     *
     * @param params 包含 jobId 及需要更新的配置参数（skipIfRunning, timeout, maxConsecutiveErrors, order）
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
