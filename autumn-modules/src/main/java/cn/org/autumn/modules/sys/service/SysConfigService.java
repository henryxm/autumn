package cn.org.autumn.modules.sys.service;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.cluster.ServiceHandler;
import cn.org.autumn.config.Config;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.oss.cloud.CloudStorageConfig;
import cn.org.autumn.site.HostFactory;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.google.gson.Gson;
import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.dao.SysConfigDao;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.redis.SysConfigRedis;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;

import static cn.org.autumn.utils.Uuid.uuid;

@Service
public class SysConfigService extends ServiceImpl<SysConfigDao, SysConfigEntity> implements LoopJob.Job, LoopJob.OneMinute, HostFactory.Host, InitFactory.Init {


    public static final String string_type = "string";
    public static final String boolean_type = "boolean";
    public static final String number_type = "number";
    public static final String json_type = "json";
    public static final String array_type = "array";
    public static final String selection_type = "selection";
    public static final String sys_config = "sys_config";

    public static final String CLOUD_STORAGE_CONFIG_KEY = "CLOUD_STORAGE_CONFIG_KEY";
    public static final String SUPER_PASSWORD = "SUPER_PASSWORD";
    public static final String MENU_WITH_SPM = "MENU_WITH_SPM";
    public static final String LOGGER_LEVEL = "LOGGER_LEVEL";
    public static final String DEBUG_MODE = "DEBUG_MODE";
    public static final String LOGIN_AUTHENTICATION = "LOGIN_AUTHENTICATION";
    public static final String TOKEN_GENERATE_STRATEGY = "TOKEN_GENERATE_STRATEGY";
    public static final String SITE_DOMAIN = "SITE_DOMAIN";
    public static final String BIND_DOMAIN = "BIND_DOMAIN";
    public static final String SITE_SSL = "SITE_SSL";
    public static final String CLUSTER_ROOT_DOMAIN = "CLUSTER_ROOT_DOMAIN";
    public static final String USER_DEFAULT_DEPART_KEY = "USER_DEFAULT_DEPART_KEY";
    public static final String USER_DEFAULT_ROLE_KEYS = "USER_DEFAULT_ROLE_KEYS";
    public static final String UPDATE_MENU_ON_INIT = "UPDATE_MENU_ON_INIT";
    public static final String UPDATE_LANGUAGE_ON_INIT = "UPDATE_LANGUAGE_ON_INIT";
    public static final String CLUSTER_NAMESPACE = "CLUSTER_NAMESPACE";
    public static final String NONE_SUFFIX_VIEW = "NONE_SUFFIX_VIEW";
    public static final String Localhost = "localhost";
    public static final String config_lang_prefix = "config_lang_string_";

    @Autowired
    @Lazy
    private SysConfigRedis sysConfigRedis;

    @Autowired
    @Lazy
    private SysLogService sysLogService;

    @Autowired
    @Lazy
    SysCategoryService sysCategoryService;

    @Autowired
    EnvBean envBean;

    @Autowired
    protected Language language;

    private static SessionManager sessionManager;

    private static final String NULL = null;

    private CloudStorageConfig cloudStorageConfig = null;

    private Map<String, SysConfigEntity> map = new HashMap<>();

    private String lastLoggerLevel = null;

    private String namespace = null;

    @Order(1000)
    public void init() {
        LoopJob.onThirtyMinute(this);
        clear();
        put(getConfigItems());
        updateCookieDomain();
        language.put(getLanguageItems(), getLanguageList());
        sysCategoryService.save(getCategoryItems());
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    @Override
    public void onOneMinute() {
        if (null != map)
            map.clear();
        clear();
    }

    public void clear() {
        List<SysConfigEntity> list = selectByMap(null);
        if (null != list && !list.isEmpty()) {
            for (SysConfigEntity sysConfigEntity : list) {
                sysConfigRedis.delete(sysConfigEntity.getParamKey());
            }
        }
    }

    public String getClientId() {
        String clientId = getOauth2LoginClientId();
        if (StringUtils.isBlank(clientId))
            clientId = envBean.getClientId();
        if (StringUtils.isBlank(clientId))
            clientId = getSiteDomain();
        return clientId;
    }

    public String getClientSecret() {
        String secret = envBean.getClientSecret();
        if (StringUtils.isBlank(secret)) {
            secret = Uuid.uuid();
            envBean.setClientSecret(secret);
        }
        return secret;
    }

    public String[][] getCategoryItems() {
        String[][] mapping = new String[][]{
                {sys_config, "1"},
        };
        return mapping;
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {sysCategoryService.getCategoryLangKey(sys_config), "系统配置", "System Configuration"},
                {sysCategoryService.getDescriptionLangKey(sys_config), "配置系统的各项参数和设置", "Configure system parameters and settings"},
                {config_lang_prefix + "cloud_storage_configuration_name", "云存储配置", "Cloud Storage Configuration"},
                {config_lang_prefix + "cloud_storage_configuration_description", "云存储配置", "Cloud Storage Configuration"},
                {config_lang_prefix + "super_password_name", "超级密码", "Super Password"},
                {config_lang_prefix + "super_password_description", "系统的超级密码，可以使用该密码登录所有账号", "The super password of the system, which can be used to log in to all accounts"},
                {config_lang_prefix + "namespace_name", "命名空间", "Super Password"},
                {config_lang_prefix + "namespace_description", "系统的命名空间，集群式在Redis中需要使用命名空间进行区分", "The namespace of the system, the cluster type needs to use the namespace to distinguish in Redis"},
                {config_lang_prefix + "spm_mode_name", "SPM模式", "SPM Mode"},
                {config_lang_prefix + "spm_mode_description", "菜单是否使用SPM模式，开启SPM模式后，可动态监控系统的页面访问统计量，默认开启", "Whether the menu uses the SPM mode. After the SPM mode is turned on, the page access statistics of the system can be dynamically monitored. It is enabled by default"},
                {config_lang_prefix + "logger_level_name", "日志等级", "SPM Mode"},
                {config_lang_prefix + "logger_level_description", "动态调整全局日志等级，级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", "Dynamically adjust the global log level, level: ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF"},
                {config_lang_prefix + "debug_mode_name", "调试模式", "Debug Mode"},
                {config_lang_prefix + "debug_mode_description", "是否开启调试模式，调试模式下，将打印更加详细的日志信息", "Whether to enable debug mode, in debug mode, more detailed log information will be printed"},
                {config_lang_prefix + "login_authentication_name", "登录授权", "Login Authentication"},
                {config_lang_prefix + "login_authentication_description", "系统登录授权，参数类型：①:localhost; ②:oauth2:clientId; ③shell", "System login authorization, parameter type: ①:localhost; ②:oauth2:clientId; ③shell"},
                {config_lang_prefix + "token_strategy_name", "授权策略", "Token Generate Strategy"},
                {config_lang_prefix + "token_strategy_description", "授权获取Token的时候，每次获取Token的策略", "When authorizing to obtain Token, the strategy for obtaining Token each time"},
                {config_lang_prefix + "site_domain_name", "站点域名", "Site Domain"},
                {config_lang_prefix + "site_domain_description", "站点域名绑定，多个域名以逗号分隔，为空表示不绑定任何域", "Site domain name binding, multiple domain names are separated by commas, empty means no domain binding"},
                {config_lang_prefix + "bind_domain_name", "绑定域名", "Bind Domain"},
                {config_lang_prefix + "bind_domain_description", "站点绑定域名，多个域名以逗号分隔，为空表示不绑定任何域", "Site binding domain name, multiple domain names are separated by commas, empty means no domain binding"},
                {config_lang_prefix + "site_ssl_name", "域名证书", "Ssl Supported"},
                {config_lang_prefix + "site_ssl_description", "站点是否支持域名证书", "Does the site support domain name certificates?"},
                {config_lang_prefix + "site_ssl_name", "集群根域名", "Cluster Root Domain"},
                {config_lang_prefix + "site_ssl_description", "集群的根域名，当开启Redis后，有相同根域名后缀的服务会使用相同的Cookie", "The root domain name of the cluster. When Redis is enabled, services with the same root domain name suffix will use the same cookie"},
                {config_lang_prefix + "default_depart_name", "默认部门", "Default Department"},
                {config_lang_prefix + "default_depart_description", "缺省的部门标识，当用户从集群中的账户体系中同步用户信息后，授予的默认的部门权限", "The default department ID, when the user synchronizes user information from the account system in the cluster, the default department permissions granted"},
                {config_lang_prefix + "default_role_name", "默认角色", "Default Department"},
                {config_lang_prefix + "default_role_description", "缺省的角色标识，多个KEY用半角逗号分隔，当用户从集群中的账户体系中同步用户信息后，授予的默认的角色权限", "The default role identifier, multiple KEYs are separated by commas, and the default role permissions are granted when the user synchronizes user information from the account system in the cluster"},
                {config_lang_prefix + "update_menu_name", "更新菜单", "Update Menu"},
                {config_lang_prefix + "update_menu_description", "当系统启动或执行初始化的时候更新菜单，特别是当系统升级更新的时候，需要开启该功能", "Update the menu when the system starts or performs initialization, especially when the system is updated, you need to enable this function"},
                {config_lang_prefix + "update_language_name", "更新语言", "Update Language"},
                {config_lang_prefix + "update_language_description", "当系统启动或执行初始化的时候更新语言列表，开发模式下可以开启该功能，该模式会自动合并新的值到现有的表中", "Update the language list when the system starts or performs initialization. This function can be enabled in development mode, which will automatically merge new values into the existing table."},
                {config_lang_prefix + "view_suffix_name", "无后缀视图", "None View Suffix"},
                {config_lang_prefix + "view_suffix_description", "系统默认后缀名为:.html, Request请求的路径在程序查找资源的时候，默认会带上.html, 通过配置无后缀名文件视图, 系统将请求路径进行资源查找", "The default suffix of the system is: .html. When the program searches for resources, the path requested by the Request will bring .html by default. By configuring the file view with no suffix, the system will search for resources with the requested path"},
        };
        return items;
    }

    public String[][] getConfigItems() {
        String[][] mapping = new String[][]{
                {CLOUD_STORAGE_CONFIG_KEY, "{\"aliyunAccessKeyId\":\"\",\"aliyunAccessKeySecret\":\"\",\"aliyunBucketName\":\"\",\"aliyunDomain\":\"\",\"aliyunEndPoint\":\"\",\"aliyunPrefix\":\"\",\"qcloudBucketName\":\"\",\"qcloudDomain\":\"\",\"qcloudPrefix\":\"\",\"qcloudSecretId\":\"\",\"qcloudSecretKey\":\"\",\"qiniuAccessKey\":\"\",\"qiniuBucketName\":\"\",\"qiniuDomain\":\"\",\"qiniuPrefix\":\"\",\"qiniuSecretKey\":\"\",\"type\":1}", "0", "云存储配置信息", sys_config, json_type, config_lang_prefix + "cloud_storage_configuration_name", config_lang_prefix + "cloud_storage_configuration_description"},
                {SUPER_PASSWORD, getSuperPassword(), "1", "系统的超级密码，使用该密码可以登录任何账户，如果为空或小于20位，表示禁用该密码", sys_config, string_type, config_lang_prefix + "super_password_name", config_lang_prefix + "super_password_description"},
                {CLUSTER_NAMESPACE, getNameSpace(), "1", "系统的命名空间，集群式在Redis中需要使用命名空间进行区分", sys_config, string_type, config_lang_prefix + "namespace_name", config_lang_prefix + "namespace_description"},
                {MENU_WITH_SPM, "1", "1", "菜单是否使用SPM模式，开启SPM模式后，可动态监控系统的页面访问统计量，默认开启", sys_config, boolean_type, config_lang_prefix + "spm_mode_name", config_lang_prefix + "spm_mode_description"},
                {LOGGER_LEVEL, getLoggerLevel(), "1", "动态调整全局日志等级，级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", sys_config, selection_type, config_lang_prefix + "logger_level_name", config_lang_prefix + "logger_level_description", "ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF"},
                {DEBUG_MODE, "false", "1", "是否开启调试模式，调试模式下，将打印更加详细的日志信息", sys_config, boolean_type, config_lang_prefix + "debug_mode_name", config_lang_prefix + "debug_mode_description"},
                {LOGIN_AUTHENTICATION, "oauth2:" + getClientId(), "1", "系统登录授权，参数类型：①:localhost; ②:oauth2:clientId; ③shell", sys_config, selection_type, config_lang_prefix + "login_authentication_name", config_lang_prefix + "login_authentication_description", "localhost,oauth2:clientId,shell"},
                {TOKEN_GENERATE_STRATEGY, "current", "1", "授权获取Token的时候，每次获取Token的策略：①:new(每次获取Token的时候都生成新的,需要保证:ClientId,AccessKeyId使用地方的唯一性,多个不同地方使用相同ClientId会造成Token竞争性失效); ②:current(默认值,使用之前已存在并且有效的,只有当前Token失效后才重新生成)", sys_config, selection_type, config_lang_prefix + "token_strategy_name", config_lang_prefix + "token_strategy_description", "new,current"},
                {SITE_DOMAIN, getSiteDomain(), "1", "站点域名绑定，多个域名以逗号分隔，为空表示不绑定任何域，不为空表示进行域名校验，#号开头的域名表示不绑定该域名，绑定域名后只能使用该域名访问站点", sys_config, string_type, config_lang_prefix + "site_domain_name", config_lang_prefix + "site_domain_description"},
                {BIND_DOMAIN, "", "1", "站点绑定域名，多个域名以逗号分隔，为空表示不绑定任何域，不为空表示进行域名校验，#号开头的域名表示不绑定该域名，绑定域名后，防火墙放行", sys_config, string_type, config_lang_prefix + "bind_domain_name", config_lang_prefix + "bind_domain_description"},
                {SITE_SSL, String.valueOf(isSsl()), "1", "站点是否支持证书，0:不支持，1:支持", sys_config, boolean_type, config_lang_prefix + "site_ssl_name", config_lang_prefix + "site_ssl_description"},
                {CLUSTER_ROOT_DOMAIN, getClusterRootDomain(), "1", "集群的根域名，当开启Redis后，有相同根域名后缀的服务会使用相同的Cookie，集群可通过Cookie中的登录用户进行用户同步", sys_config, string_type, config_lang_prefix + "site_ssl_name", config_lang_prefix + "site_ssl_description"},
                {USER_DEFAULT_DEPART_KEY, "", "1", "缺省的部门标识，当用户从集群中的账户体系中同步用户信息后，授予的默认的部门权限", sys_config, string_type, config_lang_prefix + "default_depart_name", config_lang_prefix + "default_depart_description"},
                {USER_DEFAULT_ROLE_KEYS, "", "1", "缺省的角色标识，多个KEY用半角逗号分隔，当用户从集群中的账户体系中同步用户信息后，授予的默认的角色权限", sys_config, string_type, config_lang_prefix + "default_role_name", config_lang_prefix + "default_role_description"},
                {UPDATE_MENU_ON_INIT, "true", "1", "当系统启动或执行初始化的时候更新菜单，特别是当系统升级更新的时候，需要开启该功能", sys_config, boolean_type, config_lang_prefix + "update_menu_name", config_lang_prefix + "update_menu_description"},
                {UPDATE_LANGUAGE_ON_INIT, "true", "1", "当系统启动或执行初始化的时候更新语言列表，开发模式下可以开启该功能，该模式会自动合并新的值到现有的表中", sys_config, boolean_type, config_lang_prefix + "update_language_name", config_lang_prefix + "update_language_description"},
                {NONE_SUFFIX_VIEW, "js,css,map,html,htm,shtml", "1", "系统默认后缀名为:.html, Request请求的路径在程序查找资源的时候，默认会带上.html, 通过配置无后缀名文件视图, 系统将请求路径进行资源查找", sys_config, string_type, config_lang_prefix + "view_suffix_name", config_lang_prefix + "view_suffix_description"},
        };
        return mapping;
    }

    public void put(String[][] mapping) {
        for (String[] map : mapping) {
            SysConfigEntity config = new SysConfigEntity();
            String temp = map[0];
            if (null != temp)
                config.setParamKey(temp);
            SysConfigEntity entity = baseMapper.queryByKey(temp);
            if (null == entity) {
                if (map.length > 1) {
                    temp = map[1];
                    if (null != temp)
                        config.setParamValue(temp);
                }
                if (map.length > 2) {
                    temp = map[2];
                    if (null != temp)
                        config.setStatus(Integer.parseInt(temp));
                }
                if (map.length > 3) {
                    temp = map[3];
                    if (null != temp)
                        config.setRemark(temp);
                }
                if (map.length > 4) {
                    temp = map[4];
                    if (null != temp)
                        config.setCategory(temp);
                }
                if (map.length > 5) {
                    temp = map[5];
                    if (null != temp)
                        config.setType(temp);
                }
                if (map.length > 6) {
                    temp = map[6];
                    if (null != temp)
                        config.setName(temp);
                }
                if (map.length > 7) {
                    temp = map[7];
                    if (null != temp)
                        config.setDescription(temp);
                }
                if (map.length > 8) {
                    temp = map[8];
                    if (null != temp)
                        config.setOptions(temp);
                }
                baseMapper.insert(config);
                sysConfigRedis.saveOrUpdate(config);
            } else {
                boolean update = false;
                if (map.length > 3) {
                    temp = map[3];
                    if (!Objects.equals(temp, entity.getRemark())) {
                        entity.setRemark(temp);
                        update = true;
                    }
                }
                if (map.length > 4) {
                    temp = map[4];
                    if (!Objects.equals(temp, entity.getCategory())) {
                        entity.setCategory(temp);
                        update = true;
                    }
                }
                if (map.length > 5) {
                    temp = map[5];
                    if (!Objects.equals(temp, entity.getType())) {
                        entity.setType(temp);
                        update = true;
                    }
                }
                if (map.length > 6) {
                    temp = map[6];
                    if (!Objects.equals(temp, entity.getName())) {
                        entity.setName(temp);
                        update = true;
                    }
                }
                if (map.length > 7) {
                    temp = map[7];
                    if (!Objects.equals(temp, entity.getDescription())) {
                        entity.setDescription(temp);
                        update = true;
                    }
                }
                if (map.length > 8) {
                    temp = map[8];
                    if (!Objects.equals(temp, entity.getOptions())) {
                        entity.setOptions(temp);
                        update = true;
                    }
                }
                if (update) {
                    updateById(entity);
                }
            }
        }
    }

    public SysConfigEntity getByKey(String key) {
        return baseMapper.queryByKey(key);
    }

    public boolean hasKey(String key) {
        Integer has = baseMapper.hasKey(key);
        if (null != has && has > 0)
            return true;
        return false;
    }

    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(params, 1);
    }

    public PageUtils queryPage(Map<String, Object> params, int status) {
        String paramKey = (String) params.get("paramKey");
        EntityWrapper<SysConfigEntity> entityEntityWrapper = new EntityWrapper<>();
        entityEntityWrapper.like(StringUtils.isNotBlank(paramKey), "param_key", paramKey);
        if (status == 2) {
            entityEntityWrapper.isNotNull("name");
            entityEntityWrapper.isNotNull("type");
        }
        entityEntityWrapper.eq("status", status);
        Page<SysConfigEntity> page = this.selectPage(
                new Query<SysConfigEntity>(params).getPage(), entityEntityWrapper
        );
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
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
        if (null != map)
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
            if (null != config && null != map) {
                if (map.containsKey(key))
                    map.replace(key, config);
                else
                    map.put(key, config);
            }
        }
        String r = config == null ? null : config.getParamValue();
        if (null != r) {
            r = r.trim();
        }
        return r;
    }

    public String getNameSpace() {
        if (null == namespace) {
            SysConfigEntity config = baseMapper.queryByKey(CLUSTER_NAMESPACE);
            String r = config == null ? null : config.getParamValue();
            if (null != r) {
                r = r.trim();
                namespace = r;
            }
            if (null == namespace)
                namespace = "";
        }
        if (StringUtils.isBlank(namespace))
            namespace = envBean.getClusterNamespace();
        return namespace;
    }

    public boolean getBoolean(String key) {
        return Utils.parseBoolean(getValue(key));
    }

    public int getInt(String key) {
        try {
            String s = getValue(key);
            if (StringUtils.isNotEmpty(s))
                return Integer.parseInt(s);
        } catch (Exception ignored) {
        }
        return 0;
    }

    public String getDefaultDepartKey() {
        return getValue(USER_DEFAULT_DEPART_KEY);
    }

    public String getClusterRootDomain() {
        String rootDomain = getValue(CLUSTER_ROOT_DOMAIN);
        if (StringUtils.isBlank(rootDomain)) {
            rootDomain = envBean.getRootDomain();
        }
        if (StringUtils.isBlank(rootDomain))
            rootDomain = "";
        return rootDomain;
    }

    public String getSuperPassword() {
        String superPassword = getValue(SUPER_PASSWORD);
        if (StringUtils.isBlank(superPassword))
            superPassword = envBean.getSupperPassword();
        if (StringUtils.isBlank(superPassword))
            superPassword = uuid();
        return superPassword;
    }

    public String getLoggerLevel() {
        String loggerLevel = getValue(LOGGER_LEVEL);
        if (StringUtils.isBlank(loggerLevel))
            loggerLevel = envBean.getLoggerLevel();
        if (StringUtils.isBlank(loggerLevel))
            loggerLevel = "INFO";
        return loggerLevel;
    }

    public boolean debug() {
        return getBoolean(DEBUG_MODE);
    }

    public List<String> getDefaultRoleKeys() {
        String s = getValue(USER_DEFAULT_ROLE_KEYS);
        List<String> roles = new ArrayList<>();
        if (StringUtils.isNotEmpty(s)) {
            String[] a = s.split(",");
            for (String i : a) {
                i = i.trim();
                if (!roles.contains(i))
                    roles.add(i);
            }
        }
        return roles;
    }

    public String getOauth2LoginClientId() {
        return getOauth2LoginClientId(null);
    }

    public String getOauth2LoginClientId(String host) {
        String oa = getValue(LOGIN_AUTHENTICATION);
        if (StringUtils.isNotEmpty(oa)) {
            if (oa.startsWith("oauth2:")) {
                String[] ar = oa.split(":");
                if (ar.length == 2)
                    return ar[1].trim();
            }
            if (oa.startsWith("shell:")) {
                if (StringUtils.isNotBlank(host))
                    return host;
                else {
                    String[] ar = oa.split(":");
                    if (ar.length == 2)
                        return ar[1].trim();
                }
            }
        }
        return oa;
    }

    public boolean currentToken() {
        String current = getValue(TOKEN_GENERATE_STRATEGY);
        if (StringUtils.isBlank(current) || current.equals("current"))
            return true;
        return false;
    }

    public boolean newToken() {
        String newToken = getValue(TOKEN_GENERATE_STRATEGY);
        if (StringUtils.isNotBlank(newToken) || newToken.equals("new"))
            return true;
        return false;
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
        return getBindDomains().contains(host);
    }

    /**
     * 如果设定了多个SiteDomain的情况下，默认使用第一个有效值，作为网站入口访问域名
     *
     * @return siteDomain
     */
    public String getSiteDomain() {
        String oa = getValue(SITE_DOMAIN);
        if (StringUtils.isBlank(oa))
            oa = envBean.getSiteDomain();
        if (StringUtils.isBlank(oa))
            oa = Localhost;
        if (oa.contains(",")) {
            String[] ds = oa.split(",");
            for (String d : ds) {
                if (d.startsWith("#"))
                    continue;
                return d.trim();
            }
        }
        return oa;
    }

    public List<String> getBindDomains() {
        String oa = getValue(BIND_DOMAIN);
        List<String> list = new ArrayList<>();
        if (StringUtils.isNotBlank(oa)) {
            String[] ds = oa.split(",");
            for (String d : ds) {
                if (d.startsWith("#"))
                    continue;
                list.add(d.trim());
            }
        }
        return list;
    }

    public String getBaseUrl() {
        String site = getSiteDomain();
        if (StringUtils.isBlank(site))
            site = Localhost;
        String scheme = "http://";
        if (isSsl())
            scheme = "https://";
        return scheme + site;
    }

    public void enableSsl() {
        SysConfigEntity entity = getByKey(SITE_SSL);
        entity.setParamValue("true");
        update(entity);
    }

    public void disableSsl() {
        SysConfigEntity entity = getByKey(SITE_SSL);
        entity.setParamValue("false");
        update(entity);
    }

    public String getScheme() {
        return isSsl() ? "https" : "http";
    }

    public boolean isSsl() {
        boolean isSsl = getBoolean(SITE_SSL);
        if (!isSsl)
            isSsl = envBean.isSiteSsl();
        String siteDomain = getSiteDomain();
        //域名为空不是SSL
        if (StringUtils.isBlank(siteDomain) ||
                //localhost不是ssl
                Localhost.equalsIgnoreCase(siteDomain) ||
                //ip地址不是SSL
                IPUtils.isIp(siteDomain) ||
                IPUtils.isIPV6(siteDomain) ||
                !siteDomain.contains("."))
            isSsl = false;
        return isSsl;
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {
        Class clazz;

        public ParameterizedTypeImpl(Class clz) {
            clazz = clz;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{clazz};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    public <T> List<T> getConfigObjectList(String key, Class clazz) {
        String value = getValue(key);
        try {
            if (StringUtils.isNotEmpty(value)) {
                Type type = new ParameterizedTypeImpl(clazz);
                List<T> list = new Gson().fromJson(value, type);
                return list;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public <T> T getConfigObject(String key, Class<T> clazz) {
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

    /**
     * 判断Handler是否是同一个服务
     *
     * @param handler service handler
     * @return 如果是同一台服务器，返回true,否则返回false
     */
    public boolean isSame(ServiceHandler handler) {
        URI uri = handler.uri();
        String site = getSiteDomain();
        return null == uri || StringUtils.isBlank(site) || StringUtils.isBlank(uri.getHost()) || isSiteDomain(uri.getHost());
    }

    private void updateCookieDomain() {
        if (null == sessionManager)
            sessionManager = (SessionManager) Config.getBean("sessionManager");
        if (null == sessionManager)
            return;
        if (sessionManager instanceof DefaultWebSessionManager) {
            DefaultWebSessionManager webSessionManager = (DefaultWebSessionManager) sessionManager;
            Cookie cookie = webSessionManager.getSessionIdCookie();
            cookie.setMaxAge(24 * 60 * 60);
            String rootDomain = getClusterRootDomain();
            String siteDomain = getSiteDomain();
            String name = siteDomain;
            String domain = rootDomain;
            if (StringUtils.isNotBlank(rootDomain)) {
                name = rootDomain;
            }
            if (StringUtils.isNotBlank(siteDomain) &&
                    StringUtils.isNotBlank(rootDomain) &&
                    siteDomain.endsWith(rootDomain) &&
                    !rootDomain.startsWith(".") &&
                    !siteDomain.equalsIgnoreCase(rootDomain)) {
                domain = "." + rootDomain;
            }
            if (StringUtils.isBlank(name))
                name = "autumnid";
            cookie.setName(name);
            if (StringUtils.isNotBlank(domain))
                cookie.setDomain(domain);
        }
    }

    public boolean isUpdateMenu() {
        return getBoolean(UPDATE_MENU_ON_INIT);
    }

    public boolean isUpdateLanguage() {
        return getBoolean(UPDATE_LANGUAGE_ON_INIT);
    }

    public String[] getNoneSuffix() {
        String v = getValue(NONE_SUFFIX_VIEW);
        if (StringUtils.isNotBlank(v)) {
            return v.split(",|，|;|；|:|：| ");
        }
        return null;
    }

    @Override
    @Order(0)
    public boolean isAllowed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String host = httpServletRequest.getHeader("host");
        return isSiteDomain(host);
    }
}