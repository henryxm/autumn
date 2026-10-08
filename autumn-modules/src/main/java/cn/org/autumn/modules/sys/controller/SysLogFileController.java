package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.log.LogFileService;
import cn.org.autumn.modules.sys.log.LogFileViewResult;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

/**
 * 运维：按日磁盘日志查看 / 清空。仅 /sys/logfile，不挂 /api。
 */
@Slf4j
@RestController
@RequestMapping("/sys/logfile")
public class SysLogFileController {

    @Autowired
    private LogFileService logFileService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @GetMapping("/view")
    public R view(@RequestParam(required = false) String date,
                  @RequestParam(required = false) Integer lines,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false, defaultValue = "0") Integer maxLineLength,
                  HttpServletRequest request) {
        if (!guard(request, "view")) {
            return R.error(403, "无权限");
        }
        try {
            LocalDate day = logFileService.parseDate(date);
            int maxLen = maxLineLength == null ? 0 : maxLineLength;
            LogFileViewResult result = logFileService.view(day, logFileService.clampLines(lines), keyword, maxLen);
            return R.ok()
                    .put("date", result.getDate())
                    .put("path", result.getPath())
                    .put("exists", result.isExists())
                    .put("gzip", result.isGzip())
                    .put("size", result.getSize())
                    .put("truncated", result.isTruncated())
                    .put("lines", result.getLines());
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (SecurityException e) {
            log.warn("日志查看路径拒绝: {}", e.getMessage());
            return R.error("非法路径");
        } catch (Exception e) {
            log.error("读取日志失败: {}", e.getMessage());
            return R.error("读取日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/dates")
    public R dates(HttpServletRequest request) {
        if (!guard(request, "dates")) {
            return R.error(403, "无权限");
        }
        try {
            List<String> dates = logFileService.listAvailableDates();
            return R.ok().put("dates", dates);
        } catch (Exception e) {
            log.error("列出日志日期失败: {}", e.getMessage());
            return R.error("列出日志日期失败: " + e.getMessage());
        }
    }

    @PostMapping("/clear")
    public R clear(@RequestParam String date, HttpServletRequest request) {
        if (!guard(request, "clear")) {
            return R.error(403, "无权限");
        }
        if (StringUtils.isBlank(date)) {
            return R.error("date 不能为空");
        }
        try {
            LocalDate day = logFileService.parseDate(date);
            String status = logFileService.clearDay(day);
            return R.ok().put("status", status).put("date", day.toString());
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (SecurityException e) {
            log.warn("日志清空路径拒绝: {}", e.getMessage());
            return R.error("非法路径");
        } catch (Exception e) {
            log.error("清空日志失败: {}", e.getMessage());
            return R.error("清空日志失败: " + e.getMessage());
        }
    }

    private boolean guard(HttpServletRequest request, String method) {
        if (!ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid())) {
            return false;
        }
        ipWhiteService.check(request, getClass(), method);
        return true;
    }
}
