package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.thread.FunctionQueue;
import cn.org.autumn.thread.FunctionTaskRecord;
import cn.org.autumn.thread.OverflowPolicy;
import cn.org.autumn.utils.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/sys/functionqueue")
@Endpoint(hidden = true)
@SkipInterceptor
public class SysFunctionQueueController {

    @Autowired
    @Lazy
    FunctionQueue functionQueue;

    @Autowired
    @Lazy
    SysUserRoleService sysUserRoleService;

    private boolean checkPermission() {
        return ShiroUtils.isLogin() && sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
    }

    @RequestMapping(value = "info", method = RequestMethod.POST)
    @ResponseBody
    public R info(@RequestParam(defaultValue = "50") int pendingLimit) {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        Map<String, Object> info = functionQueue.getInfo();
        return R.ok().put("queue", info).put("pending", functionQueue.peekPending(pendingLimit));
    }

    @RequestMapping(value = "status", method = RequestMethod.POST)
    @ResponseBody
    public R status() {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        return R.ok().put("queue", functionQueue.getStatus()).put("current", functionQueue.getCurrent());
    }

    @RequestMapping(value = "current", method = RequestMethod.POST)
    @ResponseBody
    public R current() {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        return R.ok().put("current", functionQueue.getCurrent());
    }

    @RequestMapping(value = "pending", method = RequestMethod.POST)
    @ResponseBody
    public R pending(@RequestParam(defaultValue = "50") int limit) {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        List<Map<String, Object>> list = functionQueue.peekPending(limit);
        return R.ok().put("list", list).put("total", functionQueue.size());
    }

    @RequestMapping(value = "history", method = RequestMethod.POST)
    @ResponseBody
    public R history(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String status, @RequestParam(required = false) String keyword) {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        List<FunctionTaskRecord> all = functionQueue.getHistory();
        List<FunctionTaskRecord> reversed = new ArrayList<>(all);
        Collections.reverse(reversed);
        List<FunctionTaskRecord> filtered = new ArrayList<>();
        for (FunctionTaskRecord record : reversed) {
            if (status != null && !status.isEmpty() && !status.equals(record.getStatus()))
                continue;
            if (keyword != null && !keyword.isEmpty()) {
                String kw = keyword.toLowerCase();
                boolean match = (record.getName() != null && record.getName().toLowerCase().contains(kw))
                        || (record.getErrorMessage() != null && record.getErrorMessage().toLowerCase().contains(kw));
                if (!match)
                    continue;
            }
            filtered.add(record);
        }
        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<FunctionTaskRecord> pageData = from < total ? filtered.subList(from, to) : Collections.emptyList();
        return R.ok().put("list", pageData).put("total", total).put("page", page).put("size", size).put("pages", size <= 0 ? 0 : (total + size - 1) / size);
    }

    @RequestMapping(value = "interrupt", method = RequestMethod.POST)
    @ResponseBody
    public R interrupt() {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        String result = functionQueue.interruptCurrent();
        if (log.isDebugEnabled())
            log.debug("Admin action - interrupt function queue: result={}, operator={}", result, ShiroUtils.getUserUuid());
        return R.ok(result);
    }

    @RequestMapping(value = "stacktrace", method = RequestMethod.POST)
    @ResponseBody
    public R stacktrace(@RequestParam(defaultValue = "40") int depth) {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        return R.ok().put("stacktrace", functionQueue.getWorkerStackTrace(depth));
    }

    @RequestMapping(value = "recover", method = RequestMethod.POST)
    @ResponseBody
    public R recover() {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        // 管理端：有当前任务则硬废弃，确保立刻进入下一任务
        String result = functionQueue.recoverIfStalled(true);
        return R.ok(result).put("queue", functionQueue.getInfo());
    }

    @RequestMapping(value = "clear", method = RequestMethod.POST)
    @ResponseBody
    public R clear() {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        int n = functionQueue.clear();
        return R.ok("已清空排队任务: " + n).put("cleared", n);
    }

    @RequestMapping(value = "clearHistory", method = RequestMethod.POST)
    @ResponseBody
    public R clearHistory() {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        functionQueue.clearHistory();
        return R.ok("历史记录已清除");
    }

    @RequestMapping(value = "resetStats", method = RequestMethod.POST)
    @ResponseBody
    public R resetStats() {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        functionQueue.resetStats();
        return R.ok("统计数据已重置").put("queue", functionQueue.getInfo());
    }

    @RequestMapping(value = "config", method = RequestMethod.POST)
    @ResponseBody
    public R updateConfig(@RequestParam(required = false) Integer capacity, @RequestParam(required = false) String overflowPolicy, @RequestParam(required = false) Integer maxHistorySize, @RequestParam(required = false) Long slowTaskThresholdMs, @RequestParam(required = false) Long maxTaskTimeoutMs, @RequestParam(required = false) Long hardAbandonMs) {
        if (!checkPermission())
            return R.error(403, "无权限访问");
        try {
            OverflowPolicy policy = null;
            if (overflowPolicy != null && !overflowPolicy.isEmpty()) {
                try {
                    policy = OverflowPolicy.valueOf(overflowPolicy.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return R.error("溢出策略无效: REJECT / DROP_OLDEST");
                }
            }
            if (capacity != null && (capacity < 1 || capacity > 1000000))
                return R.error("容量范围: 1 - 1000000");
            if (maxHistorySize != null && (maxHistorySize < 10 || maxHistorySize > 100000))
                return R.error("最大历史记录数范围: 10 - 100000");
            if (slowTaskThresholdMs != null && (slowTaskThresholdMs < 1000 || slowTaskThresholdMs > 3600000))
                return R.error("慢任务阈值范围: 1000 - 3600000 毫秒");
            if (maxTaskTimeoutMs != null && (maxTaskTimeoutMs < 1000 || maxTaskTimeoutMs > 3600000))
                return R.error("任务硬超时范围: 1000 - 3600000 毫秒");
            if (hardAbandonMs != null && (hardAbandonMs < 500 || hardAbandonMs > 300000))
                return R.error("硬废弃宽限范围: 500 - 300000 毫秒");
            functionQueue.updateConfig(capacity, policy, maxHistorySize, slowTaskThresholdMs, maxTaskTimeoutMs, hardAbandonMs);
            if (log.isDebugEnabled())
                log.debug("FunctionQueue config updated - capacity:{}, policy:{}, maxHistory:{}, slowMs:{}, maxTaskMs:{}, abandonMs:{}", capacity, overflowPolicy, maxHistorySize, slowTaskThresholdMs, maxTaskTimeoutMs, hardAbandonMs);
            return R.ok("配置更新成功").put("queue", functionQueue.getInfo());
        } catch (Exception e) {
            log.error("Failed to update function queue config:{}", e.getMessage());
            return R.error("配置更新失败: " + e.getMessage());
        }
    }
}
