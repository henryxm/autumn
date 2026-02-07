package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.thread.TaskRecord;
import cn.org.autumn.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/sys/thread")
@Endpoint(hidden = true)
@SkipInterceptor
public class ThreadController {

    @Autowired
    @Lazy
    TagTaskExecutor tagTaskExecutor;

    @Autowired
    @Lazy
    SysUserRoleService sysUserRoleService;

    /**
     * 权限检查：仅系统管理员可访问
     */
    private boolean checkPermission() {
        return ShiroUtils.isLogin() && sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
    }

    /**
     * 获取线程池综合信息（池信息 + 运行中任务）
     */
    @RequestMapping(value = "info", method = RequestMethod.POST)
    @ResponseBody
    public R info() {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        Map<String, Object> poolInfo = tagTaskExecutor.getPoolInfo();
        List<Map<String, Object>> runningDetails = tagTaskExecutor.getRunningDetails();
        return R.ok().put("pool", poolInfo).put("running", runningDetails);
    }

    /**
     * 获取线程池基本状态（轻量级，用于定时刷新）
     */
    @RequestMapping(value = "status", method = RequestMethod.POST)
    @ResponseBody
    public R status() {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        return R.ok().put("pool", tagTaskExecutor.getPoolInfo());
    }

    /**
     * 获取运行中的任务列表
     */
    @RequestMapping(value = "running", method = RequestMethod.POST)
    @ResponseBody
    public R running() {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        List<Map<String, Object>> list = tagTaskExecutor.getRunningDetails();
        return R.ok().put("list", list).put("total", list.size());
    }

    /**
     * 获取任务执行历史（分页）
     */
    @RequestMapping(value = "history", method = RequestMethod.POST)
    @ResponseBody
    public R history(@RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "20") int size,
                     @RequestParam(required = false) String status,
                     @RequestParam(required = false) String keyword) {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        List<TaskRecord> allHistory = tagTaskExecutor.getHistory();
        List<TaskRecord> reversed = new ArrayList<>(allHistory);
        Collections.reverse(reversed);
        // 过滤
        List<TaskRecord> filtered = new ArrayList<>();
        for (TaskRecord record : reversed) {
            if (status != null && !status.isEmpty() && !status.equals(record.getStatus())) {
                continue;
            }
            if (keyword != null && !keyword.isEmpty()) {
                String kw = keyword.toLowerCase();
                boolean match = (record.getName() != null && record.getName().toLowerCase().contains(kw))
                        || (record.getTag() != null && record.getTag().toLowerCase().contains(kw))
                        || (record.getMethod() != null && record.getMethod().toLowerCase().contains(kw))
                        || (record.getTypeName() != null && record.getTypeName().toLowerCase().contains(kw));
                if (!match) continue;
            }
            filtered.add(record);
        }
        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<TaskRecord> pageData = from < total ? filtered.subList(from, to) : Collections.emptyList();
        return R.ok()
                .put("list", pageData)
                .put("total", total)
                .put("page", page)
                .put("size", size)
                .put("pages", (total + size - 1) / size);
    }

    /**
     * 中断指定任务（按索引）
     */
    @RequestMapping(value = "interrupt", method = RequestMethod.POST)
    @ResponseBody
    public R interrupt(@RequestParam int index) {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        String result = tagTaskExecutor.interruptTaskByIndex(index);
        if (log.isDebugEnabled())
            log.debug("管理员操作 - 中断任务: index={}, result={}, operator={}", index, result, ShiroUtils.getUserUuid());
        return R.ok(result);
    }

    /**
     * 按任务ID中断任务
     */
    @RequestMapping(value = "interruptById", method = RequestMethod.POST)
    @ResponseBody
    public R interruptById(@RequestParam String taskId) {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        String result = tagTaskExecutor.interruptTask(taskId);
        if (log.isDebugEnabled())
            log.debug("管理员操作 - 中断任务: taskId={}, result={}, operator={}", taskId, result, ShiroUtils.getUserUuid());
        return R.ok(result);
    }

    /**
     * 获取指定任务的线程堆栈（用于诊断卡死/死锁）
     */
    @RequestMapping(value = "stacktrace", method = RequestMethod.POST)
    @ResponseBody
    public R stacktrace(@RequestParam int index, @RequestParam(defaultValue = "20") int depth) {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        String stack = tagTaskExecutor.getTaskStackTrace(index, depth);
        return R.ok().put("stacktrace", stack);
    }

    /**
     * 动态修改线程池配置
     */
    @RequestMapping(value = "config", method = RequestMethod.POST)
    @ResponseBody
    public R updateConfig(@RequestParam(required = false) Integer corePoolSize,
                          @RequestParam(required = false) Integer maxPoolSize,
                          @RequestParam(required = false) Long staggerSeconds,
                          @RequestParam(required = false) Integer maxHistorySize) {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        try {
            if (corePoolSize != null) {
                if (corePoolSize < 1 || corePoolSize > 100000) {
                    return R.error("核心线程数范围: 1 - 100000");
                }
                tagTaskExecutor.updateCorePoolSize(corePoolSize);
            }
            if (maxPoolSize != null) {
                if (maxPoolSize < 1 || maxPoolSize > 100000) {
                    return R.error("最大线程数范围: 1 - 100000");
                }
                tagTaskExecutor.updateMaxPoolSize(maxPoolSize);
            }
            if (staggerSeconds != null) {
                if (staggerSeconds < 0 || staggerSeconds > 3600) {
                    return R.error("全局错峰延迟范围: 0 - 3600 秒");
                }
                TagTaskExecutor.setGlobalStaggerSeconds(staggerSeconds);
            }
            if (maxHistorySize != null) {
                if (maxHistorySize < 10 || maxHistorySize > 100000) {
                    return R.error("最大历史记录数范围: 10 - 100000");
                }
                TagTaskExecutor.setMaxHistorySize(maxHistorySize);
            }
            if (log.isDebugEnabled())
                log.debug("线程池配置已更新 - corePoolSize:{}, maxPoolSize:{}, staggerSeconds:{}, maxHistorySize:{}", corePoolSize, maxPoolSize, staggerSeconds, maxHistorySize);
            return R.ok("配置更新成功").put("pool", tagTaskExecutor.getPoolInfo());
        } catch (Exception e) {
            log.error("更新线程池配置失败:{}", e.getMessage());
            return R.error("配置更新失败: " + e.getMessage());
        }
    }

    /**
     * 清除历史记录
     */
    @RequestMapping(value = "clearHistory", method = RequestMethod.POST)
    @ResponseBody
    public R clearHistory() {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        tagTaskExecutor.clearHistory();
        if (log.isDebugEnabled())
            log.debug("线程池历史记录已清除");
        return R.ok("历史记录已清除");
    }

    /**
     * 重置统计数据
     */
    @RequestMapping(value = "resetStats", method = RequestMethod.POST)
    @ResponseBody
    public R resetStats() {
        if (!checkPermission()) {
            return R.error(403, "无权限访问");
        }
        tagTaskExecutor.resetStats();
        if (log.isDebugEnabled())
            log.debug("线程池统计数据已重置");
        return R.ok("统计数据已重置").put("pool", tagTaskExecutor.getPoolInfo());
    }

    /**
     * 兼容旧接口
     */
    @RequestMapping(value = "threading", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> postThreading() {
        if (!checkPermission()) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> executor = new HashMap<>();
        executor.put("ActiveCount", tagTaskExecutor.getActiveCount());
        executor.put("CorePoolSize", tagTaskExecutor.getCorePoolSize());
        executor.put("MaxPoolSize", tagTaskExecutor.getMaxPoolSize());
        executor.put("PoolSize", tagTaskExecutor.getPoolSize());
        map.put("Executor", executor);
        map.put("Tags", tagTaskExecutor.getRunning());
        return map;
    }
}
