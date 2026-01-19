package cn.org.autumn.modules.sys.service;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.cluster.ServiceHandler;
import cn.org.autumn.config.*;
import cn.org.autumn.exception.AException;
import cn.org.autumn.model.AesConfig;
import cn.org.autumn.model.RsaConfig;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.oss.cloud.CloudStorageConfig;
import cn.org.autumn.modules.sys.dao.SysConfigDao;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.entity.SystemUpgrade;
import cn.org.autumn.modules.sys.redis.SysConfigRedis;
import cn.org.autumn.site.ConfigFactory;
import cn.org.autumn.site.DomainFactory;
import cn.org.autumn.site.HostFactory;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

import static cn.org.autumn.utils.Uuid.uuid;

@Slf4j
@Service
public class SysConfigService extends ServiceImpl<SysConfigDao, SysConfigEntity> implements LoopJob.Job, LoopJob.OneMinute, HostFactory.Host, InitFactory.Init, InitFactory.After, CategoryHandler, DomainHandler, ClearHandler {

    public static final String string_type = InputType.StringType.getValue();
    public static final String boolean_type = InputType.BooleanType.getValue();
    public static final String number_type = InputType.NumberType.getValue();
    public static final String json_type = InputType.JsonType.getValue();
    public static final String array_type = InputType.ArrayType.getValue();
    public static final String selection_type = InputType.SelectionType.getValue();
    public static final String config = "sys_config";

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
    public static final String SYSTEM_UPGRADE = "SYSTEM_UPGRADE";
    public static final String RSA_CONFIG = "RSA_CONFIG";
    public static final String AES_CONFIG = "AES_CONFIG";
    public static final String Localhost = "localhost";
    public static final String config_lang_prefix = "config_lang_string_";
    private static final String NULL = null;
    private static SessionManager sessionManager;
    @Autowired
    protected Language language;
    @Autowired
    @Lazy
    SysCategoryService sysCategoryService;
    @Autowired
    EnvBean envBean;
    @Autowired
    ConfigFactory configFactory;

    @Autowired
    AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    DomainFactory domainFactory;
    List<String[]> lang = new ArrayList<>();
    @Autowired
    @Lazy
    private SysConfigRedis sysConfigRedis;
    @Autowired
    @Lazy
    private SysLogService sysLogService;
    private CloudStorageConfig cloudStorageConfig = null;
    private Map<String, SysConfigEntity> map = new HashMap<>();
    private String lastLoggerLevel = null;
    private String namespace = null;
    private static final String temp = Uuid.uuid();

    @Order(-1000)
    public void init() {
        LoopJob.onThirtyMinute(this);
        clear();
        put(getConfigItems());
        updateCookieDomain();
        language.put(getLanguageItems(), getLanguageList());
    }

    @SuppressWarnings("RedundantArrayCreation")
    @Override
    public void after() {
        language.put(false, new Object[]{lang, getLanguageList()});
        lang.clear();
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    @Override
    public void onOneMinute() {
        if (null != map)
            map.clear();
        domainFactory.clear();
    }

    public void clear() {
        String configKey = RedisKeys.getConfigPrefix(getNameSpace());
        Set<String> keys = stringRedisTemplate.keys(configKey + "*");
        if (null != keys && !keys.isEmpty())
            stringRedisTemplate.delete(keys);
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
        return new String[][]{
                {config, "1"},
                {SystemUpgrade.config, "1"},
        };
    }

    public String[][] getLanguageItems() {
        return new String[][]{
                {categoryName(config), "系统配置", "System Configuration"},
                {categoryDescription(config), "配置系统的各项参数和设置", "Configure system parameters and settings"},
                {configName(CLOUD_STORAGE_CONFIG_KEY), "云存储配置", "Cloud Storage Configuration"},
                {configDescription(CLOUD_STORAGE_CONFIG_KEY), "云存储配置", "Cloud Storage Configuration"},
                {configName(SUPER_PASSWORD), "超级密码", "Super Password"},
                {configDescription(SUPER_PASSWORD), "系统的超级密码，可以使用该密码登录所有账号", "The super password of the system, which can be used to log in to all accounts"},
                {configName(CLUSTER_NAMESPACE), "命名空间", "Super Password"},
                {configDescription(CLUSTER_NAMESPACE), "系统的命名空间，集群式在Redis中需要使用命名空间进行区分", "The namespace of the system, the cluster type needs to use the namespace to distinguish in Redis"},
                {configName(MENU_WITH_SPM), "SPM模式", "SPM Mode"},
                {configDescription(MENU_WITH_SPM), "菜单是否使用SPM模式，开启SPM模式后，可动态监控系统的页面访问统计量，默认开启", "Whether the menu uses the SPM mode. After the SPM mode is turned on, the page access statistics of the system can be dynamically monitored. It is enabled by default"},
                {configName(LOGGER_LEVEL), "日志等级", "SPM Mode"},
                {configDescription(LOGGER_LEVEL), "动态调整全局日志等级，级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", "Dynamically adjust the global log level, level: ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF"},
                {configName(DEBUG_MODE), "调试模式", "Debug Mode"},
                {configDescription(DEBUG_MODE), "是否开启调试模式，调试模式下，将打印更加详细的日志信息", "Whether to enable debug mode, in debug mode, more detailed log information will be printed"},
                {configName(LOGIN_AUTHENTICATION), "登录授权", "Login Authentication"},
                {configDescription(LOGIN_AUTHENTICATION), "系统登录授权，参数类型：①:localhost; ②:oauth2:clientId; ③shell", "System login authorization, parameter type: ①:localhost; ②:oauth2:clientId; ③shell"},
                {configName(TOKEN_GENERATE_STRATEGY), "授权策略", "Token Generate Strategy"},
                {configDescription(TOKEN_GENERATE_STRATEGY), "授权获取Token的时候，每次获取Token的策略", "When authorizing to obtain Token, the strategy for obtaining Token each time"},
                {configName(SITE_DOMAIN), "站点域名", "Site Domain"},
                {configDescription(SITE_DOMAIN), "站点域名绑定，多个域名以逗号分隔，为空表示不绑定任何域", "Site domain name binding, multiple domain names are separated by commas, empty means no domain binding"},
                {configName(BIND_DOMAIN), "绑定域名", "Bind Domain"},
                {configDescription(BIND_DOMAIN), "站点绑定域名，多个域名以逗号分隔，为空表示不绑定任何域", "Site binding domain name, multiple domain names are separated by commas, empty means no domain binding"},
                {configName(SITE_SSL), "传输加密", "Transmission encryption"},
                {configDescription(SITE_SSL), "站点是否支持SSL加密传输", "Whether the site supports SSL encrypted transmission?"},
                {configName(CLUSTER_ROOT_DOMAIN), "集群根域名", "Cluster Root Domain"},
                {configDescription(CLUSTER_ROOT_DOMAIN), "集群的根域名，当开启Redis后，有相同根域名后缀的服务会使用相同的Cookie", "The root domain name of the cluster. When Redis is enabled, services with the same root domain name suffix will use the same cookie"},
                {configName(USER_DEFAULT_DEPART_KEY), "默认部门", "Default Department"},
                {configDescription(USER_DEFAULT_DEPART_KEY), "缺省的部门标识，当用户从集群中的账户体系中同步用户信息后，授予的默认的部门权限", "The default department ID, when the user synchronizes user information from the account system in the cluster, the default department permissions granted"},
                {configName(USER_DEFAULT_ROLE_KEYS), "默认角色", "Default Role"},
                {configDescription(USER_DEFAULT_ROLE_KEYS), "缺省的角色标识，多个KEY用半角逗号分隔，当用户从集群中的账户体系中同步用户信息后，授予的默认的角色权限", "The default role identifier, multiple KEYs are separated by commas, and the default role permissions are granted when the user synchronizes user information from the account system in the cluster"},
                {configName(UPDATE_MENU_ON_INIT), "更新菜单", "Update Menu"},
                {configDescription(UPDATE_MENU_ON_INIT), "当系统启动或执行初始化的时候更新菜单，特别是当系统升级更新的时候，需要开启该功能", "Update the menu when the system starts or performs initialization, especially when the system is updated, you need to enable this function"},
                {configName(UPDATE_LANGUAGE_ON_INIT), "更新语言", "Update Language"},
                {configDescription(UPDATE_LANGUAGE_ON_INIT), "当系统启动或执行初始化的时候更新语言列表，开发模式下可以开启该功能，该模式会自动合并新的值到现有的表中", "Update the language list when the system starts or performs initialization. This function can be enabled in development mode, which will automatically merge new values into the existing table."},
                {configName(NONE_SUFFIX_VIEW), "无后缀视图", "None View Suffix"},
                {configDescription(NONE_SUFFIX_VIEW), "系统默认后缀名为:.html, Request请求的路径在程序查找资源的时候，默认会带上.html, 通过配置无后缀名文件视图, 系统将请求路径进行资源查找", "The default suffix of the system is: .html. When the program searches for resources, the path requested by the Request will bring .html by default. By configuring the file view with no suffix, the system will search for resources with the requested path"},
                {configName(SYSTEM_UPGRADE), "系统升级", "System Upgrade"},
                {configDescription(SYSTEM_UPGRADE), "系统升级过程启用开关，当升级时，部分功能被禁止，比如上传数据，新增数据", "The switch is enabled during the system upgrade process. When upgrading, some functions are prohibited, such as uploading data and adding data"},
        };
    }

    public String[][] getConfigItems() {
        return new String[][]{
                {LOGIN_AUTHENTICATION, "oauth2:" + getClientId(), "0", "系统登录授权，参数类型：①:localhost; ②:oauth2:" + getClientId() + "; ③shell:" + getClientId(), config, selection_type, "localhost,oauth2:" + getClientId() + ",shell:" + getClientId()},
                {SITE_DOMAIN, getDefaultSiteDomains(), "1", "站点域名绑定，多个域名以逗号分隔，为空表示不绑定任何域，不为空表示进行域名校验，#号开头的域名表示不绑定该域名，绑定域名后只能使用该域名访问站点", config, string_type},
                {BIND_DOMAIN, "", "1", "站点绑定域名，多个域名以逗号分隔，为空表示不绑定任何域，不为空表示进行域名校验，#号开头的域名表示不绑定该域名，绑定域名后，防火墙放行", config, string_type},
                {CLUSTER_ROOT_DOMAIN, getClusterRootDomain(), "1", "集群的根域名，当开启Redis后，有相同根域名后缀的服务会使用相同的Cookie，集群可通过Cookie中的登录用户进行用户同步", config, string_type},
                {CLUSTER_NAMESPACE, getNameSpace(), "1", "系统的命名空间，集群式在Redis中需要使用命名空间进行区分", config, string_type},
                {SUPER_PASSWORD, getSuperPassword(), "0", "系统的超级密码，使用该密码可以登录任何账户，如果为空或小于20位，表示禁用该密码", config, string_type},
                {MENU_WITH_SPM, "1", "1", "菜单是否使用SPM模式，开启SPM模式后，可动态监控系统的页面访问统计量，默认开启", config, boolean_type},
                {LOGGER_LEVEL, getLoggerLevel(), "1", "动态调整全局日志等级，级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", config, selection_type, "ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF"},
                {DEBUG_MODE, "false", "1", "是否开启调试模式，调试模式下，将打印更加详细的日志信息", config, boolean_type},
                {TOKEN_GENERATE_STRATEGY, "current", "0", "授权获取Token的时候，每次获取Token的策略：①:new(每次获取Token的时候都生成新的,需要保证:ClientId,AccessKeyId使用地方的唯一性,多个不同地方使用相同ClientId会造成Token竞争性失效); ②:current(默认值,使用之前已存在并且有效的,只有当前Token失效后才重新生成)", config, selection_type, "new,current"},
                {SITE_SSL, String.valueOf(isSsl()), "1", "站点是否支持证书，0:不支持，1:支持", config, boolean_type},
                {USER_DEFAULT_DEPART_KEY, "", "1", "缺省的部门标识，当用户从集群中的账户体系中同步用户信息后，授予的默认的部门权限", config, string_type},
                {USER_DEFAULT_ROLE_KEYS, "", "1", "缺省的角色标识，多个KEY用半角逗号分隔，当用户从集群中的账户体系中同步用户信息后，授予的默认的角色权限", config, string_type},
                {UPDATE_MENU_ON_INIT, "true", "1", "当系统启动或执行初始化的时候更新菜单，特别是当系统升级更新的时候，需要开启该功能", config, boolean_type},
                {UPDATE_LANGUAGE_ON_INIT, "true", "1", "当系统启动或执行初始化的时候更新语言列表，开发模式下可以开启该功能，该模式会自动合并新的值到现有的表中", config, boolean_type},
                {NONE_SUFFIX_VIEW, "js,css,map,html,htm,shtml", "0", "系统默认后缀名为:.html, Request请求的路径在程序查找资源的时候，默认会带上.html, 通过配置无后缀名文件视图, 系统将请求路径进行资源查找", config, string_type},
                {SYSTEM_UPGRADE, new Gson().toJson(new SystemUpgrade()), "1", "系统升级开关与提示信息", config, json_type, SystemUpgrade.class.getName()},
                {CLOUD_STORAGE_CONFIG_KEY, "{\"aliyunAccessKeyId\":\"\",\"aliyunAccessKeySecret\":\"\",\"aliyunBucketName\":\"\",\"aliyunDomain\":\"\",\"aliyunEndPoint\":\"\",\"aliyunPrefix\":\"\",\"qcloudBucketName\":\"\",\"qcloudDomain\":\"\",\"qcloudPrefix\":\"\",\"qcloudSecretId\":\"\",\"qcloudSecretKey\":\"\",\"qiniuAccessKey\":\"\",\"qiniuBucketName\":\"\",\"qiniuDomain\":\"\",\"qiniuPrefix\":\"\",\"qiniuSecretKey\":\"\",\"type\":1}", "0", "云存储配置信息", config, json_type, CloudStorageConfig.class.getName()},
                {RSA_CONFIG, new Gson().toJson(new RsaConfig()), "1", "RSA加密配置", config, json_type, RsaConfig.class.getName()},
                {AES_CONFIG, new Gson().toJson(new AesConfig()), "1", "AES加密配置", config, json_type, AesConfig.class.getName()},
        };
    }

    public void put(String[][] mapping) {
        for (String[] map : mapping) {
            SysConfigEntity config = new SysConfigEntity();
            String temp = map[0];
            if (null != temp)
                config.setParamKey(temp);
            SysConfigEntity entity = baseMapper.queryByKey(temp);
            SysConfigEntity langEntity = null;
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
                } else {
                    config.setCategory(SysConfigService.config);
                }
                if (map.length > 5) {
                    temp = map[5];
                    if (null != temp)
                        config.setType(temp);
                } else {
                    config.setType(string_type);
                }
                if (map.length > 6) {
                    temp = map[6];
                    if (null != temp)
                        config.setOptions(temp);
                } else {
                    config.setOptions("");
                }
                if (map.length > 7) {
                    temp = map[7];
                    if (null != temp)
                        config.setReadonly(Utils.parseBoolean(temp));
                } else {
                    config.setReadonly(false);
                }
                config.setName(configName(config.getParamKey()));
                config.setDescription(configDescription(config.getParamKey()));
                baseMapper.insert(config);
                sysConfigRedis.saveOrUpdate(config);
                langEntity = config;
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
                } else {
                    update = true;
                    entity.setCategory(SysConfigService.config);
                }
                if (map.length > 5) {
                    temp = map[5];
                    if (!Objects.equals(temp, entity.getType())) {
                        entity.setType(temp);
                        update = true;
                    }
                } else {
                    update = true;
                    entity.setType(string_type);
                }
                if (map.length > 6) {
                    temp = map[6];
                    if (!Objects.equals(temp, entity.getOptions())) {
                        entity.setOptions(temp);
                        update = true;
                    }
                } else {
                    entity.setOptions("");
                    update = true;
                }
                if (map.length > 7) {
                    temp = map[7];
                    if (null != temp) {
                        config.setReadonly(Utils.parseBoolean(temp));
                        update = true;
                    }
                }
                if (update) {
                    entity.setName(configName(entity.getParamKey()));
                    entity.setDescription(configDescription(entity.getParamKey()));
                    updateById(entity);
                }
                langEntity = entity;
            }

            String name = langEntity.getRemark();
            if (StringUtils.isNotBlank(name)) {
                if (name.contains(":")) {
                    name = name.split(":")[0];
                } else {
                    if (name.length() > 8) {
                        name = name.substring(0, 8);
                    }
                }
            }
            if (StringUtils.isNotBlank(name)) {
                String[] names = new String[]{langEntity.getName(), name};
                lang.add(names);
            }
            if (StringUtils.isNotBlank(langEntity.getRemark())) {
                String[] desc = new String[]{langEntity.getDescription(), langEntity.getRemark()};
                lang.add(desc);
            }
        }
    }

    public SystemUpgrade getSystemUpgrade() {
        return getConfigObject(SYSTEM_UPGRADE, SystemUpgrade.class);
    }

    public SysConfigEntity getByKey(String key) {
        return baseMapper.queryByKey(key);
    }

    public boolean hasKey(String key) {
        Integer has = baseMapper.hasKey(key);
        return null != has && has > 0;
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
            sysLogService.changeLevel(config.getParamValue(), NULL);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysConfigEntity config) {
        this.updateAllColumnById(config);
        sysConfigRedis.saveOrUpdate(config);
        if (LOGGER_LEVEL.equalsIgnoreCase(config.getParamKey())) {
            sysLogService.changeLevel(config.getParamValue(), NULL);
        }
        asyncTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                runJob();
                configFactory.update(config.getParamKey(), config.getParamValue());
            }
        });
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
            sysLogService.changeLevel(value, NULL);
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
        }
        if (StringUtils.isBlank(namespace)) {
            namespace = envBean.getClusterNamespace();
        }
        if (null == namespace) {
            namespace = "";
        }
        return namespace;
    }

    public boolean getBoolean(String key) {
        return Utils.parseBoolean(getValue(key));
    }

    public int getInt(String key) {
        try {
            String s = getValue(key);
            if (StringUtils.isNotEmpty(s)) {
                return Integer.parseInt(s);
            }
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
        if (StringUtils.isBlank(rootDomain)) {
            rootDomain = getSiteDomain();
        }
        if (null == rootDomain)
            rootDomain = "";
        return rootDomain;
    }

    public String getSuperPassword() {
        String superPassword = getValue(SUPER_PASSWORD);
        if (StringUtils.isBlank(superPassword)) {
            superPassword = envBean.getSupperPassword();
        }
        if (StringUtils.isBlank(superPassword) || superPassword.length() < 20) {
            superPassword = temp;
        }
        return superPassword;
    }

    public String getLoggerLevel() {
        String loggerLevel = getValue(LOGGER_LEVEL);
        if (StringUtils.isBlank(loggerLevel)) {
            loggerLevel = envBean.getLoggerLevel();
        }
        if (StringUtils.isBlank(loggerLevel)) {
            loggerLevel = "INFO";
        }
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
                if (!roles.contains(i)) {
                    roles.add(i);
                }
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
                if (ar.length == 2) {
                    return ar[1].trim();
                }
            }
            if (oa.startsWith("shell:")) {
                if (StringUtils.isNotBlank(host)) {
                    return host;
                } else {
                    String[] ar = oa.split(":");
                    if (ar.length == 2) {
                        return ar[1].trim();
                    }
                }
            }
        }
        return oa;
    }

    public boolean currentToken() {
        String current = getValue(TOKEN_GENERATE_STRATEGY);
        return StringUtils.isBlank(current) || "current".equals(current);
    }

    public boolean newToken() {
        String newToken = getValue(TOKEN_GENERATE_STRATEGY);
        return StringUtils.isNotBlank(newToken) || "new".equals(newToken);
    }

    public boolean isSuperPassword(String password) {
        return Objects.equals(getSuperPassword(), password);
    }

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

    public String getDefaultSiteDomains() {
        String siteDomain = envBean.getSiteDomain();
        if (StringUtils.isBlank(siteDomain)) {
            String ip = IPUtils.getIp();
            if (StringUtils.isNotBlank(ip)) {
                siteDomain = ip + "," + Localhost + "," + "127.0.0.1";
            } else {
                siteDomain = Localhost + "," + "127.0.0.1";
            }
        }
        return siteDomain;
    }

    /**
     * 如果设定了多个SiteDomain的情况下，默认使用第一个有效值，作为网站入口访问域名
     *
     * @return siteDomain
     */
    public String getSiteDomain() {
        String oa = getValue(SITE_DOMAIN);
        if (StringUtils.isBlank(oa)) {
            oa = getDefaultSiteDomains();
        }
        if (oa.contains(",")) {
            String[] ds = oa.split(",");
            for (String d : ds) {
                if (d.startsWith("#")) {
                    continue;
                }
                return d.trim();
            }
        }
        return oa;
    }

    @Override
    public boolean isBindDomain(String domain) {
        if (StringUtils.isBlank(domain))
            return false;
        domain = domain.toLowerCase().trim();
        return getBindDomains().contains(domain);
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

    public <T> List<T> getConfigObjectList(String key, Class<?> clazz) {
        String value = getValue(key);
        try {
            if (StringUtils.isNotEmpty(value)) {
                Type type = new ParameterizedTypeImpl(clazz);
                return new Gson().fromJson(value, type);
            }
        } catch (Exception ignored) {
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

    public <T> T getConfigObjectValidate(String key, Class<T> clazz) {
        T config = getConfigObject(key, clazz);
        // 对RSA和AES配置进行校验和修正
        if (config != null) {
            List<String> fixes = null;
            if (RsaConfig.class.isAssignableFrom(clazz) && config instanceof RsaConfig) {
                fixes = ((RsaConfig) config).validateAndFix();
            } else if (AesConfig.class.isAssignableFrom(clazz) && config instanceof AesConfig) {
                fixes = ((AesConfig) config).validateAndFix();
            }
            // 如果配置被修正，记录日志并更新到数据库
            if (fixes != null && !fixes.isEmpty()) {
                for (String fix : fixes) {
                    log.info("[配置校验] " + fix);
                }
                String newValue = new Gson().toJson(config);
                updateValueByKey(key, newValue);
            }
        }
        return config;
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
                        sysLogService.changeLevel(sysConfigEntity.getParamValue(), NULL);
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
        return null == uri || StringUtils.isBlank(site) || StringUtils.isBlank(uri.getHost()) || domainFactory.isSiteBind(uri.getHost());
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

    public void inject(Iterator<String> fields, Class<?> clazz, Field field, Object obj, Object parent, String name, String value) throws IllegalAccessException {
        if (fields.hasNext()) {
            String next = fields.next();
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field1 : declaredFields) {
                if (Objects.equals(field1.getName(), next)) {
                    field1.setAccessible(true);
                    inject(fields, field1.getType(), field1, field1.get(obj), obj, next, value);
                    break;
                }
            }
        } else {
            ConfigField configField = field.getDeclaredAnnotation(ConfigField.class);
            if (null != configField && Objects.equals(field.getName(), name)) {
                if (configField.category().getValue().equals(boolean_type)) {
                    field.set(parent, Boolean.valueOf(value));
                } else if (configField.category().getValue().equals(number_type) || configField.category().getValue().equals(InputType.IntegerType.getValue())) {
                    field.set(parent, Integer.parseInt(value));
                } else if (configField.category().getValue().equals(InputType.LongType.getValue())) {
                    field.set(parent, Long.parseLong(value));
                } else if (configField.category().getValue().equals(InputType.FloatType.getValue())) {
                    field.set(parent, Float.parseFloat(value));
                } else if (configField.category().getValue().equals(InputType.DoubleType.getValue())) {
                    field.set(parent, Double.parseDouble(value));
                } else if (configField.category().getValue().equals(InputType.DecimalType.getValue())) {
                    field.set(parent, BigDecimal.valueOf(Double.parseDouble(value)));
                } else
                    field.set(parent, value);
            }
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
        if (null != host && host.contains(":"))
            host = host.split(":")[0];
        return domainFactory.isSiteBind(host);
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {
        Class<?> clazz;

        public ParameterizedTypeImpl(Class<?> clz) {
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
}