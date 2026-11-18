package cn.org.autumn.modules.ops.service;

import cn.org.autumn.modules.spm.entity.VisitLogEntity;
import cn.org.autumn.modules.spm.service.VisitLogService;
import cn.org.autumn.modules.sys.service.RedisService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.service.IpAdminWhiteService;
import cn.org.autumn.modules.wall.service.IpBlackService;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.modules.wall.service.ShieldService;
import cn.org.autumn.database.runtime.WrapperColumns;
import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Qualifier;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 运维运行快照：分段采集，单段失败不影响整包。
 */
@Slf4j
@Service
public class OpsRuntimeSnapshotService {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private IpAdminWhiteService ipAdminWhiteService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @Autowired
    private IpBlackService ipBlackService;

    @Autowired
    private ShieldService shieldService;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    @Qualifier("firstDataSource")
    private DataSource firstDataSource;

    @Autowired(required = false)
    private VisitLogService visitLogService;

    @Autowired(required = false)
    private RedisService redisService;

    @Autowired(required = false)
    private OpsRuntimeTrafficCounter trafficCounter;

    @Value("${autumn.backup.dir:#{systemProperties['user.home'] + '/database'}}")
    private String backupDir;

    public Map<String, Object> snapshot() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("collectedAt", System.currentTimeMillis());
        root.put("system", safe("system", this::collectSystem));
        root.put("jvm", safe("jvm", this::collectJvm));
        root.put("disk", safe("disk", this::collectDisk));
        root.put("database", safe("database", this::collectDatabase));
        root.put("firewall", safe("firewall", this::collectFirewall));
        root.put("traffic", safe("traffic", this::collectTraffic));
        root.put("redis", safe("redis", this::collectRedis));
        root.put("paths", safe("paths", this::collectPaths));
        root.put("exceptions", Map.of("supported", false, "note", "无历史异常仓库；攻击线索见 firewall"));
        return root;
    }

    private Map<String, Object> safe(String section, SupplierMap supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("runtime snapshot section={} failed: {}", section, e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("available", false);
            err.put("error", e.getMessage());
            return err;
        }
    }

    @FunctionalInterface
    private interface SupplierMap {
        Map<String, Object> get() throws Exception;
    }

    Map<String, Object> collectSystem() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", true);
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        m.put("osName", os.getName());
        m.put("osArch", os.getArch());
        m.put("osVersion", os.getVersion());
        m.put("availableProcessors", os.getAvailableProcessors());
        m.put("systemLoadAverage", os.getSystemLoadAverage());
        try {
            m.put("hostname", InetAddress.getLocalHost().getHostName());
        } catch (Exception ignored) {
            m.put("hostname", "");
        }
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            double processCpu = sunOs.getProcessCpuLoad();
            double systemCpu = sunOs.getCpuLoad();
            if (processCpu >= 0) {
                m.put("processCpuLoad", round4(processCpu));
            }
            if (systemCpu >= 0) {
                m.put("systemCpuLoad", round4(systemCpu));
            }
            long totalMem = sunOs.getTotalMemorySize();
            long freeMem = sunOs.getFreeMemorySize();
            if (totalMem > 0) {
                m.put("physicalTotalBytes", totalMem);
                m.put("physicalFreeBytes", freeMem);
                m.put("physicalUsedBytes", totalMem - freeMem);
            }
        }
        return m;
    }

    Map<String, Object> collectJvm() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", true);
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        m.put("uptimeMs", rt.getUptime());
        m.put("startTimeMs", rt.getStartTime());
        m.put("vmName", rt.getVmName());
        m.put("vmVersion", rt.getVmVersion());
        m.put("javaVersion", System.getProperty("java.version"));
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        m.put("heap", memoryMap(mem.getHeapMemoryUsage()));
        m.put("nonHeap", memoryMap(mem.getNonHeapMemoryUsage()));
        ThreadMXBean th = ManagementFactory.getThreadMXBean();
        m.put("threadLive", th.getThreadCount());
        m.put("threadDaemon", th.getDaemonThreadCount());
        m.put("threadPeak", th.getPeakThreadCount());
        Runtime runtime = Runtime.getRuntime();
        m.put("runtimeTotalBytes", runtime.totalMemory());
        m.put("runtimeFreeBytes", runtime.freeMemory());
        m.put("runtimeMaxBytes", runtime.maxMemory());
        return m;
    }

    private Map<String, Object> memoryMap(MemoryUsage u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("init", u.getInit());
        m.put("used", u.getUsed());
        m.put("committed", u.getCommitted());
        m.put("max", u.getMax());
        return m;
    }

    Map<String, Object> collectDisk() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", true);
        List<Map<String, Object>> volumes = new ArrayList<>();
        volumes.add(diskEntry("workdir", Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()));
        volumes.add(diskEntry("temp", Paths.get(System.getProperty("java.io.tmpdir", "/tmp")).toAbsolutePath().normalize()));
        if (StringUtils.isNotBlank(backupDir)) {
            volumes.add(diskEntry("backup", Paths.get(backupDir).toAbsolutePath().normalize()));
        }
        m.put("volumes", volumes);
        return m;
    }

    private Map<String, Object> diskEntry(String name, Path path) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("name", name);
        e.put("path", path.toString());
        try {
            Path probe = path;
            if (!Files.exists(probe)) {
                Path parent = probe.getParent();
                if (parent != null && Files.exists(parent)) {
                    probe = parent;
                } else {
                    e.put("available", false);
                    e.put("error", "路径不存在");
                    return e;
                }
            }
            FileStore store = Files.getFileStore(probe);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            e.put("available", true);
            e.put("totalBytes", total);
            e.put("usableBytes", usable);
            e.put("usedBytes", Math.max(0, total - usable));
        } catch (Exception ex) {
            e.put("available", false);
            e.put("error", ex.getMessage());
        }
        return e;
    }

    Map<String, Object> collectDatabase() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        if (dataSource == null) {
            m.put("available", false);
            m.put("error", "DataSource 未注入");
            return m;
        }
        m.put("available", true);
        DruidDataSource druid = resolveDruid();
        if (druid != null) {
            m.put("poolType", "druid");
            m.put("activeCount", druid.getActiveCount());
            m.put("poolingCount", druid.getPoolingCount());
            m.put("waitThreadCount", druid.getWaitThreadCount());
            m.put("maxActive", druid.getMaxActive());
            m.put("minIdle", druid.getMinIdle());
        } else {
            m.put("poolType", dataSource.getClass().getSimpleName());
        }
        try (Connection cn = dataSource.getConnection()) {
            try {
                cn.setNetworkTimeout(Runnable::run, 8_000);
            } catch (Exception ignored) {
                // 部分驱动不支持
            }
            DatabaseMetaData meta = cn.getMetaData();
            m.put("productName", meta.getDatabaseProductName());
            m.put("productVersion", meta.getDatabaseProductVersion());
            m.put("url", sanitizeJdbcUrl(meta.getURL()));
            int tables = 0;
            try (ResultSet rs = meta.getTables(cn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables++;
                }
            }
            m.put("tableCount", tables);
        } catch (Exception e) {
            m.put("tableCount", -1);
            m.put("tableCountError", e.getMessage());
        }
        return m;
    }

    private DruidDataSource resolveDruid() {
        if (firstDataSource instanceof DruidDataSource d) {
            return d;
        }
        if (dataSource instanceof DruidDataSource d) {
            return d;
        }
        return null;
    }

    private String sanitizeJdbcUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        int q = url.indexOf('?');
        return q > 0 ? url.substring(0, q) : url;
    }

    Map<String, Object> collectFirewall() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", true);
        m.put("adminIpEnable", sysConfigService.isAdminIpEnable());
        m.put("adminIpCount", safeCount(() -> ipAdminWhiteService.count()));
        m.put("ipWhiteCount", safeCount(() -> ipWhiteService.count()));
        m.put("ipBlackCount", safeCount(() -> ipBlackService.count() - (ipBlackService.hasIp(IpBlackService.POLICY_IP) ? 1 : 0)));
        m.put("firewallCount", ipBlackService.getFirewallCount());
        m.put("shieldAttack", ShieldService.getAttack());
        m.put("shieldPrint", ShieldService.getPrint());
        m.put("shieldEnabled", shieldService.getEnable());
        m.put("shieldVisitWindow", shieldService.getVisitWindowSize());
        m.put("shieldAllowedIpCount", shieldService.getAllowedIpSize());
        m.put("shieldAllowedIpSample", shieldService.getAllowedIpSample(20));
        m.put("blackTopToday", blackTopToday(10));
        return m;
    }

    private long safeCount(CountFn fn) {
        try {
            return fn.get();
        } catch (Exception e) {
            return -1;
        }
    }

    @FunctionalInterface
    private interface CountFn {
        long get();
    }

    private List<Map<String, Object>> blackTopToday(int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            QueryWrapper<IpBlackEntity> qw = new QueryWrapper<>();
            qw.ne("ip", IpBlackService.POLICY_IP);
            qw.orderByDesc("today");
            Page<IpBlackEntity> page = ipBlackService.page(new Page<>(1, Math.max(1, Math.min(limit, 50))), qw);
            List<IpBlackEntity> list = page.getRecords();
            for (IpBlackEntity e : list) {
                if (e.getToday() == null || e.getToday() <= 0) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ip", e.getIp());
                row.put("today", e.getToday());
                row.put("count", e.getCount());
                row.put("tag", e.getTag());
                row.put("available", e.getAvailable());
                out.add(row);
            }
        } catch (Exception e) {
            log.debug("blackTopToday failed: {}", e.getMessage());
        }
        return out;
    }

    Map<String, Object> collectTraffic() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", true);
        if (trafficCounter != null) {
            m.put("requestTotal", trafficCounter.getTotal());
            m.put("approxQps", round4(trafficCounter.getApproxQps()));
        } else {
            m.put("requestTotal", 0);
            m.put("approxQps", 0);
        }
        if (visitLogService != null) {
            try {
                String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
                QueryWrapper<VisitLogEntity> qw = WrapperColumns.queryWrapperAllEqQuoted(Map.of("day_string", day));
                List<VisitLogEntity> logs = visitLogService.list(qw);
                long pv = 0;
                long uv = 0;
                for (VisitLogEntity v : logs) {
                    if (v.getPageView() != null) {
                        pv += v.getPageView();
                    }
                    if (v.getUniqueVisitor() != null) {
                        uv += v.getUniqueVisitor();
                    }
                }
                m.put("visitLogDay", day);
                m.put("pageViewToday", pv);
                m.put("uniqueVisitorToday", uv);
                m.put("visitLogRowsToday", logs.size());
            } catch (Exception e) {
                m.put("visitLogError", e.getMessage());
            }
        }
        return m;
    }

    Map<String, Object> collectRedis() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (redisService == null) {
            m.put("enabled", false);
            m.put("available", false);
            return m;
        }
        try {
            Map<String, Object> info = redisService.getServerInfo();
            boolean connected = Boolean.TRUE.equals(info.get("connected"));
            m.put("enabled", true);
            m.put("available", connected);
            m.putAll(info);
            if (connected) {
                List<Map<String, Object>> dbs = redisService.getDatabases();
                if (!dbs.isEmpty()) {
                    m.put("keyCount", dbs.get(0).get("keyCount"));
                    m.put("usedMemoryBytes", dbs.get(0).get("size"));
                }
            }
        } catch (Exception e) {
            m.put("enabled", true);
            m.put("available", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    Map<String, Object> collectPaths() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", true);
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(pathItem("工作目录", System.getProperty("user.dir", ".")));
        list.add(pathItem("临时目录", System.getProperty("java.io.tmpdir", "")));
        list.add(pathItem("备份目录", backupDir));
        String home = System.getProperty("user.home", "");
        if (StringUtils.isNotBlank(home)) {
            list.add(pathItem("用户目录", home));
        }
        File logCandidate = new File("logs");
        if (logCandidate.exists()) {
            list.add(pathItem("日志目录", logCandidate.getAbsolutePath()));
        }
        m.put("items", list);
        return m;
    }

    private Map<String, Object> pathItem(String label, String path) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("label", label);
        e.put("path", path == null ? "" : path);
        return e;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
