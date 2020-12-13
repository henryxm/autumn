package cn.org.autumn.modules.sys.service;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.oss.cloud.CloudStorageConfig;
import cn.org.autumn.modules.wall.service.WallService;
import cn.org.autumn.site.HostFactory;
import cn.org.autumn.table.TableInit;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.google.gson.Gson;
import cn.org.autumn.exception.AException;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.sys.dao.SysConfigDao;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.redis.SysConfigRedis;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static cn.org.autumn.utils.Uuid.uuid;

@Service
public class SysConfigService extends ServiceImpl<SysConfigDao, SysConfigEntity> implements LoopJob.Job, HostFactory.Host {

    public static final String CLOUD_STORAGE_CONFIG_KEY = "CLOUD_STORAGE_CONFIG_KEY";
    public static final String SUPER_PASSWORD = "SUPER_PASSWORD";
    public static final String MENU_WITH_SPM = "MENU_WITH_SPM";
    public static final String LOGGER_LEVEL = "LOGGER_LEVEL";
    public static final String LOGIN_AUTHENTICATION = "LOGIN_AUTHENTICATION";
    public static final String SITE_DOMAIN = "SITE_DOMAIN";
    public static final String CLUSTER_ROOT_DOMAIN = "CLUSTER_ROOT_DOMAIN";
    public static final String USER_DEFAULT_DEPART_ID = "USER_DEFAULT_DEPART_ID";
    public static final String USER_DEFAULT_ROLE_IDS = "USER_DEFAULT_ROLE_IDS";

    @Autowired
    private SysConfigRedis sysConfigRedis;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private SysLogService sysLogService;

    private static SessionManager sessionManager;

    private static final String NULL = null;

    @Autowired
    private TableInit tableInit;

    private CloudStorageConfig cloudStorageConfig = null;

    private Map<String, SysConfigEntity> map;

    private String lastLoggerLevel = null;

    @PostConstruct
    public void init() {
        LoopJob.onOneMinute(this);
        if (!tableInit.init)
            return;
        String[][] mapping = new String[][]{
                {CLOUD_STORAGE_CONFIG_KEY, "{\"aliyunAccessKeyId\":\"\",\"aliyunAccessKeySecret\":\"\",\"aliyunBucketName\":\"\",\"aliyunDomain\":\"\",\"aliyunEndPoint\":\"\",\"aliyunPrefix\":\"\",\"qcloudBucketName\":\"\",\"qcloudDomain\":\"\",\"qcloudPrefix\":\"\",\"qcloudSecretId\":\"\",\"qcloudSecretKey\":\"\",\"qiniuAccessKey\":\"\",\"qiniuBucketName\":\"\",\"qiniuDomain\":\"\",\"qiniuPrefix\":\"\",\"qiniuSecretKey\":\"\",\"type\":1}", "0", "云存储配置信息"},
                {SUPER_PASSWORD, uuid(), "1", "系统的超级密码，使用该密码可以登录任何账户，如果为空或小于20位，表示禁用该密码"},
                {MENU_WITH_SPM, "1", "1", "菜单是否使用SPM模式，开启SPM模式后，可动态监控系统的页面访问统计量，默认开启"},
                {LOGGER_LEVEL, "INFO", "1", "动态调整全局日志等级，级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF"},
                {LOGIN_AUTHENTICATION, "oauth2:" + WebAuthenticationService.clientId, "1", "系统登录授权，参数类型：①:localhost; ②:oauth2:clientId"},
                {SITE_DOMAIN, "", "1", "站点域名绑定，多个域名以逗号分隔，为空表示不绑定任何域，不为空表示进行域名校验，#号开头的域名表示不绑定该域名，绑定域名后只能使用该域名访问站点"},
                {CLUSTER_ROOT_DOMAIN, "autumn.org.cn", "1", "集群的根域名，当开启Redis后，有相同根域名后缀的服务会使用相同的Cookie，集群可通过Cookie中的登录用户进行用户同步"},
                {USER_DEFAULT_DEPART_ID, "1", "1", "缺省的部门ID，当用户从集群中的账户体系中同步用户信息后，授予的默认的部门权限"},
                {USER_DEFAULT_ROLE_IDS, "0", "1", "缺省的角色IDs，多个ID用半角逗号分隔，当用户从集群中的账户体系中同步用户信息后，授予的默认的角色权限"},
        };
        for (String[] map : mapping) {
            SysConfigEntity sysMenu = new SysConfigEntity();
            String temp = map[0];
            if (NULL != temp)
                sysMenu.setParamKey(temp);
            SysConfigEntity entity = sysConfigDao.queryByKey(temp);
            if (null == entity) {
                temp = map[1];
                if (NULL != temp)
                    sysMenu.setParamValue(temp);
                temp = map[2];
                if (NULL != temp)
                    sysMenu.setStatus(Integer.valueOf(temp));
                temp = map[3];
                if (NULL != temp)
                    sysMenu.setRemark(temp);
                sysConfigDao.insert(sysMenu);
            }
        }
    }

    public PageUtils queryPage(Map<String, Object> params) {
        String paramKey = (String) params.get("paramKey");

        Page<SysConfigEntity> page = this.selectPage(
                new Query<SysConfigEntity>(params).getPage(),
                new EntityWrapper<SysConfigEntity>()
                        .like(StringUtils.isNotBlank(paramKey), "param_key", paramKey)
                        .eq("status", 1)
        );

        return new PageUtils(page);
    }

    public void save(SysConfigEntity config) {
        this.insert(config);
        sysConfigRedis.saveOrUpdate(config);
        if (LOGGER_LEVEL.equalsIgnoreCase(config.getParamKey())) {
            sysLogService.changeLevel(config.getParamValue(), NULL, NULL);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysConfigEntity config) {
        this.updateAllColumnById(config);
        sysConfigRedis.saveOrUpdate(config);
        if (LOGGER_LEVEL.equalsIgnoreCase(config.getParamKey())) {
            sysLogService.changeLevel(config.getParamValue(), NULL, NULL);
        }
        runJob();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateValueByKey(String key, String value) {
        baseMapper.updateValueByKey(key, value);
        sysConfigRedis.delete(key);
        if (map.containsKey(key))
            map.remove(key);
        if (CLOUD_STORAGE_CONFIG_KEY.equalsIgnoreCase(key))
            cloudStorageConfig = null;
        if (LOGGER_LEVEL.equalsIgnoreCase(key))
            sysLogService.changeLevel(value, NULL, NULL);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] ids) {
        for (Long id : ids) {
            SysConfigEntity config = this.selectById(id);
            sysConfigRedis.delete(config.getParamKey());
        }
        this.deleteBatchIds(Arrays.asList(ids));
        runJob();
    }

    public String getValue(String key) {
        SysConfigEntity config = null;
        if (null != map && map.containsKey(key)) {
            config = map.get(key);
        }
        if (null == config)
            config = sysConfigRedis.get(key);
        if (config == null) {
            config = baseMapper.queryByKey(key);
            sysConfigRedis.saveOrUpdate(config);
        }
        return config == null ? null : config.getParamValue();
    }

    public Boolean getBoolean(String key) {
        String s = getValue(key);
        if (StringUtils.isEmpty(s))
            return false;
        if ("1".equalsIgnoreCase(s))
            return true;
        return Boolean.valueOf(s);
    }

    public Integer getInt(String key) {
        try {
            String s = getValue(key);
            if (StringUtils.isNotEmpty(s))
                return Integer.valueOf(s);
        } catch (Exception e) {
        }
        return 0;
    }

    public Integer getDefaultDepartId() {
        return getInt(USER_DEFAULT_DEPART_ID);
    }

    public String getClusterRootDomain() {
        return getValue(CLUSTER_ROOT_DOMAIN);
    }

    public List<Long> getDefaultRoleIds() {
        String s = getValue(USER_DEFAULT_ROLE_IDS);
        List<Long> roles = new ArrayList<>();
        if (StringUtils.isNotEmpty(s)) {
            String[] a = s.split(",");
            for (String i : a) {
                i = i.trim();
                try {
                    Long r = Long.valueOf(i);
                    if (r <= 0)
                        continue;
                    if (!roles.contains(r))
                        roles.add(r);
                } catch (Exception e) {
                }
            }
        }
        return roles;
    }

    public String getOauth2LoginClientId() {
        String oa = getValue(LOGIN_AUTHENTICATION);
        if (StringUtils.isNotEmpty(oa) && oa.startsWith("oauth2:")) {
            String[] ar = oa.split(":");
            if (ar.length == 2)
                return ar[1].trim();
        }
        return "";
    }

    /**
     * 超级密码校验
     * 超级密码必须不小于20位
     *
     * @param password
     * @return
     */
    public boolean isSuperPassword(String password) {
        if (StringUtils.isEmpty(password) || password.length() < 20)
            return false;

        String oa = getValue(SUPER_PASSWORD);
        if (StringUtils.isEmpty(oa) || oa.length() < 20)
            return false;

        return password.equals(oa);
    }

    /**
     * 校验绑定域名函数
     *
     * @param host
     * @return
     */
    public boolean isSiteDomain(String host) {
        if (StringUtils.isEmpty(host))
            return false;

        String oa = getValue(SITE_DOMAIN);
        if (StringUtils.isEmpty(oa))
            return true;

        String[] ds = oa.split(",");
        for (String d : ds) {
            if (d.startsWith("#"))
                continue;
            if (d.trim().equalsIgnoreCase(host.trim()))
                return true;
        }
        return false;
    }

    private <T> T getConfigObject(String key, Class<T> clazz) {
        String value = getValue(key);
        if (StringUtils.isNotBlank(value)) {
            return new Gson().fromJson(value, clazz);
        }

        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new AException("获取参数失败");
        }
    }

    public CloudStorageConfig getCloudStorageConfig() {
        if (null == cloudStorageConfig)
            cloudStorageConfig = getConfigObject(CLOUD_STORAGE_CONFIG_KEY, CloudStorageConfig.class);
        return cloudStorageConfig;
    }

    @Override
    public void runJob() {
        List<SysConfigEntity> list = selectByMap(new HashMap<>());
        if (null != list && list.size() > 0) {
            Map<String, SysConfigEntity> t = new HashMap<>();
            for (SysConfigEntity sysConfigEntity : list) {
                t.put(sysConfigEntity.getParamKey(), sysConfigEntity);
                if (LOGGER_LEVEL.equalsIgnoreCase(sysConfigEntity.getParamKey())) {
                    if (null == lastLoggerLevel || !lastLoggerLevel.equalsIgnoreCase(sysConfigEntity.getParamValue()))
                        sysLogService.changeLevel(sysConfigEntity.getParamValue(), NULL, NULL);
                    lastLoggerLevel = sysConfigEntity.getParamValue();
                }
            }
            map = t;
            cloudStorageConfig = null;
            updateCookieDomain();
        }
    }

    private void updateCookieDomain() {
        if (null == sessionManager)
            sessionManager = (SessionManager) Config.getBean("sessionManager");
        if (null == sessionManager)
            return;
        String rootDomain = getClusterRootDomain();
        if (StringUtils.isEmpty(rootDomain))
            return;
        if (sessionManager instanceof DefaultWebSessionManager) {
            DefaultWebSessionManager webSessionManager = (DefaultWebSessionManager) sessionManager;
            Cookie cookie = webSessionManager.getSessionIdCookie();
            if (null != cookie) {
                if (!rootDomain.startsWith("."))
                    rootDomain = "." + rootDomain;
                if (StringUtils.isEmpty(cookie.getDomain()) || !cookie.getDomain().equalsIgnoreCase(rootDomain))
                    cookie.setDomain(rootDomain);
            }
        }
    }

    @Override
    public boolean isAllowed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String host = httpServletRequest.getHeader("host");
        return isSiteDomain(host);
    }
}
