package cn.org.autumn.modules.wall.service;

import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.config.Config;
import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.exception.AException;
import cn.org.autumn.model.IP;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.dao.IpAdminWhiteDao;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import cn.org.autumn.modules.wall.entity.RData;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.thread.FunctionQueues;
import cn.org.autumn.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理级 IP 白名单：运维页 / 超管 API / gen 全表门禁。
 * <p>
 * 与用户表 {@code wall_ip_white} 物理分离；不跟随 {@code WallFactory.isIpWhiteEnable()}；
 * <strong>不从用户白名单迁移</strong>，初始化为空，空名单时走一次性临时密钥引导。
 * 用户防火墙 {@link IpWhiteService#isWhite} 逻辑上继承本名单（管理 IP 默认免拉黑）。
 */
@Slf4j
@Service
public class IpAdminWhiteService extends WallCounter<IpAdminWhiteDao, IpAdminWhiteEntity>
        implements InitFactory.Init, LoadFactory.Load, LoopJob.OneMinute, ClearHandler {

    public static final String BOOTSTRAP_LOG_PREFIX = "AdminIpBootstrap secret=";

    private final SecureRandom secureRandom = new SecureRandom();

    private volatile Set<String> adminIpList = new HashSet<>();
    private volatile Set<String> adminIpSectionList = new HashSet<>();

    /** 引导盐与 hash；认领成功或名单非空时均为 null。永不存明文。 */
    private volatile byte[] bootstrapSalt;
    private volatile byte[] bootstrapHash;

    @Override
    public String[][] getMenuItems() {
        return null;
    }

    @Override
    protected String[][] getMenuItemsInternal() {
        return null;
    }

    @Override
    protected List<String[]> getLanguageItemsInternal() {
        return null;
    }

    @Override
    public String ico() {
        return "fa-shield";
    }

    public void load() {
        try {
            Set<String> ips = baseMapper.getIps(0);
            if (ips == null) {
                ips = new HashSet<>();
            }
            Set<String> sections = new HashSet<>();
            for (String ip : ips) {
                if (StringUtils.isNotBlank(ip) && ip.contains("/")) {
                    sections.add(ip);
                }
            }
            adminIpList = ips;
            adminIpSectionList = sections;
        } catch (Exception e) {
            log.error("加载管理级IP白名单失败 msg={}", e.getMessage());
        }
    }

    public boolean isEmpty() {
        try {
            return baseMapper.activeCount() <= 0;
        } catch (Exception e) {
            return adminIpList == null || adminIpList.isEmpty();
        }
    }

    public boolean isBootstrapPending() {
        return bootstrapHash != null && bootstrapSalt != null && isEmpty();
    }

    public boolean isAdminWhite(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        if (adminIpList.contains(ip)) {
            return true;
        }
        for (String section : adminIpSectionList) {
            if (IPUtils.isInRange(ip, section)) {
                return true;
            }
        }
        return false;
    }

    public IpAdminWhiteEntity getByIp(String ip) {
        return baseMapper.getByIp(ip);
    }

    public boolean hasIp(String ip) {
        return baseMapper.hasIp(ip) > 0;
    }

    public void put(String ip) {
        if (StringUtils.isNotBlank(ip)) {
            adminIpList.add(ip);
            if (ip.contains("/")) {
                adminIpSectionList.add(ip);
            }
        }
    }

    public void removeByIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return;
        }
        adminIpList.remove(ip);
        adminIpSectionList.remove(ip);
        IpAdminWhiteEntity entity = getByIp(ip);
        if (entity != null) {
            baseMapper.deleteById(entity.getId());
        }
    }

    /**
     * 管理门禁：非 dev 须在管理白名单。不绑定用户防火墙开关，不回退用户白名单。
     */
    public void checkAdmin(HttpServletRequest servlet, Class<?> clazz, String method) {
        if (Config.isDev()) {
            return;
        }
        if (servlet == null) {
            log.warn("SecurityAlert denied reason=admin_ip_white, detail=request_null, target={}",
                    (clazz != null ? clazz.getSimpleName() : "-") + "#" + StringUtils.defaultIfBlank(method, "-"));
            throw new AException("服务繁忙");
        }
        String ip = IP.getIp(servlet);
        if (isAdminWhite(ip)) {
            return;
        }
        String user = "-";
        try {
            if (ShiroUtils.isLogin()) {
                user = StringUtils.defaultIfBlank(ShiroUtils.getUserUuid(), "-");
            }
        } catch (Exception ignored) {
        }
        String entry = (clazz != null ? clazz.getSimpleName() : "-") + "#" + StringUtils.defaultIfBlank(method, "-");
        log.warn("SecurityAlert denied reason=admin_ip_white, user={}, login={}, ip={}, uri={}, target={}, detail=管理IP未加白, ua={}",
                user, ShiroUtils.isLogin(), ip, StringUtils.defaultIfBlank(servlet.getRequestURI(), "-"), entry,
                StringUtils.abbreviate(StringUtils.defaultString(servlet.getHeader("User-Agent")), 120));
        throw new AException("服务繁忙");
    }

    /**
     * 允许管理 IP（含内网/本机，便于运维认领与局域网管理）。
     */
    public IpAdminWhiteEntity create(String ip, String tag, String description) {
        return create(ip, tag, description, "", false);
    }

    public IpAdminWhiteEntity create(String ip, String tag, String description, String userAgent, boolean update) {
        if (!IPUtils.isIp(ip) && !IPUtils.isIPV6(ip) && (ip == null || !ip.contains("/"))) {
            return null;
        }
        CrudGuard.opt(() -> {
            try {
                IpAdminWhiteEntity entity = getByIp(ip);
                if (entity == null) {
                    entity = new IpAdminWhiteEntity();
                    entity.setIp(ip);
                    entity.setTag(tag);
                    entity.setUserAgent(userAgent);
                    entity.setDescription(description);
                    entity.setCreateTime(new Date());
                    entity.setForbidden(0);
                    entity.setCount(0L);
                    entity.setToday(0L);
                    save(entity);
                } else if (update) {
                    entity.setTag(tag);
                    entity.setUserAgent(userAgent);
                    entity.setDescription(description);
                    entity.setUpdateTime(new Date());
                    updateById(entity);
                }
                put(ip);
            } catch (Exception e) {
                if (!CrudGuard.suppress(e, "添加管理白名单IP " + ip)) {
                    log.error("添加管理白名单IP失败 ip={} msg={}", ip, e.getMessage());
                }
            }
        });
        return getByIp(ip);
    }

    /**
     * 空名单时生成一次性临时密钥：控制台打印明文，内存只存 salt+hash。
     */
    public synchronized void ensureBootstrapSecretIfEmpty() {
        load();
        if (!isEmpty()) {
            clearBootstrapSecret();
            return;
        }
        if (bootstrapHash != null && bootstrapSalt != null) {
            return;
        }
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        byte[] plain = new byte[32];
        secureRandom.nextBytes(plain);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(plain);
        byte[] hash = sha256(salt, secret);
        this.bootstrapSalt = salt;
        this.bootstrapHash = hash;
        log.warn("{}{}", BOOTSTRAP_LOG_PREFIX, secret);
    }

    /**
     * 用临时密钥认领当前请求 IP。成功后立即清除密钥（一次性）。
     *
     * @return 写入的实体；失败抛 AException
     */
    public synchronized IpAdminWhiteEntity bootstrapClaim(HttpServletRequest request, String secret) {
        if (!isEmpty()) {
            clearBootstrapSecret();
            throw new AException("管理IP白名单已初始化", 403);
        }
        if (bootstrapHash == null || bootstrapSalt == null) {
            throw new AException("引导密钥不可用", 403);
        }
        if (StringUtils.isBlank(secret)) {
            log.warn("SecurityAlert denied reason=admin_ip_bootstrap, detail=密钥为空, ip={}",
                    request != null ? IP.getIp(request) : "-");
            throw new AException("引导密钥无效", 403);
        }
        byte[] got = sha256(bootstrapSalt, secret.trim());
        if (!MessageDigest.isEqual(got, bootstrapHash)) {
            log.warn("SecurityAlert denied reason=admin_ip_bootstrap, detail=密钥错误, ip={}",
                    request != null ? IP.getIp(request) : "-");
            throw new AException("引导密钥无效", 403);
        }
        String ip = request != null ? IP.getIp(request) : null;
        if (StringUtils.isBlank(ip)) {
            throw new AException("无法解析客户端IP", 403);
        }
        IpAdminWhiteEntity entity = create(ip, "bootstrap", "启动临时密钥认领");
        if (entity == null) {
            // 未写入则保留 hash 供重试
            throw new AException("写入管理IP失败", 500);
        }
        // 写库成功后立即清密钥（一次性），控制台旧明文失效
        clearBootstrapSecret();
        load();
        log.info("管理IP白名单引导完成 ip={}", ip);
        return entity;
    }

    public void clearBootstrapSecret() {
        bootstrapSalt = null;
        bootstrapHash = null;
    }

    /** 测试用：是否仍持有引导 hash（不暴露内容）。 */
    public boolean hasBootstrapHash() {
        return bootstrapHash != null;
    }

    private static byte[] sha256(byte[] salt, String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(secret.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public void init() {
        load();
        ensureBootstrapSecretIfEmpty();
    }

    @Override
    public void onOneMinute() {
        FunctionQueues.offer("IpAdminWhiteService.clear", this::clear);
    }

    @Override
    protected void save(String key, RData rData) {
        baseMapper.count(key, rData.getUserAgent(), rData.getCount());
    }

    @Override
    public void refresh() {
        baseMapper.refresh();
    }

    @Override
    public void clear() {
        load();
        if (isEmpty()) {
            ensureBootstrapSecretIfEmpty();
        } else {
            clearBootstrapSecret();
        }
    }

    @Override
    protected boolean has(String key) {
        return hasIp(key);
    }
}
