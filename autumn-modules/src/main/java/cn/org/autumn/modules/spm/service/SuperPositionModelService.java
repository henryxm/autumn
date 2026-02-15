package cn.org.autumn.modules.spm.service;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.site.*;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.service.gen.SuperPositionModelServiceGen;
import cn.org.autumn.modules.sys.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SuperPositionModelService extends SuperPositionModelServiceGen implements LoadFactory.Load, LoopJob.TenMinute, LoginFactory.Login, PathFactory.Path, ClearHandler {

    private static final Map<String, SuperPositionModelEntity> models = new ConcurrentHashMap<>();

    @Autowired
    AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    VisitLogService visitLogService;

    @Autowired
    SiteFactory siteFactory;

    @Autowired
    LoginFactory loginFactory;

    @Autowired
    PathFactory pathFactory;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    LoadFactory loadFactory;

    @Autowired
    MappingFactory mappingFactory;

    private static final Map<String, String> spmListForHtml = new ConcurrentHashMap<>();
    private static final Map<String, SuperPositionModelEntity> spmListForUrlKey = new ConcurrentHashMap<>();
    private static final Map<String, SuperPositionModelEntity> spmListForResourceID = new ConcurrentHashMap<>();

    @Override
    public int menuOrder() {
        return 7;
    }

    @Override
    public String ico() {
        return "fa-location-arrow";
    }

    public Map<String, String> getSpmListForHtml() {
        return spmListForHtml;
    }

    public Map<String, SuperPositionModelEntity> getSpmListForResourceID() {
        return spmListForResourceID;
    }

    public void load() {
        site();
        List<SuperPositionModelEntity> list = baseMapper.selectByMap(new HashMap<>());
        for (SuperPositionModelEntity superPositionModelEntity : list) {
            spmListForHtml.put(superPositionModelEntity.getUrlKey(), "spm=" + superPositionModelEntity.toSpmString());
            spmListForUrlKey.put(superPositionModelEntity.getUrlKey(), superPositionModelEntity);
            spmListForResourceID.put(superPositionModelEntity.getResourceId(), superPositionModelEntity);
        }
    }

    public void site() {
        Collection<SiteFactory.Site> sites = siteFactory.getSites();
        if (null == sites)
            return;
        for (SiteFactory.Site site : sites) {

            Field[] fields = site.getClass().getDeclaredFields();
            for (Field field : fields) {
                PageAware aware = field.getAnnotation(PageAware.class);
                if (null == aware)
                    continue;
                String siteId = site.getId();
                String page = field.getName();

                if (StringUtils.isNotEmpty(aware.page()) && !"NULL".equalsIgnoreCase(aware.page()) && !"0".equalsIgnoreCase(aware.page()))
                    page = aware.page();

                String channel = aware.channel();
                String product = aware.product();

                String resource = page;
                if (StringUtils.isNotEmpty(aware.resource()))
                    resource = aware.resource();
                String url = resource;
                if (StringUtils.isNotEmpty(aware.url()))
                    url = aware.url();
                if (StringUtils.isEmpty(resource) || page.equals(resource)) {
                    try {
                        field.setAccessible(true);
                        Object v = field.get(site);
                        if (v instanceof String) {
                            String value = (String) v;
                            if (StringUtils.isNotEmpty(value))
                                resource = value;
                        }
                    } catch (Exception e) {
                        log.error("Error:{}", e.getMessage());
                    }
                }
                boolean login = aware.login();
                String key = site.getKey(field.getName());
                put(siteId, page, channel, product, resource, url, key, login);
            }
        }
    }

    public String getResourceId(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        if (!loadFactory.isDone()) {
            if (log.isDebugEnabled())
                log.debug("启动中:{}", httpServletRequest.getRequestURL());
            return pageFactory.loading(httpServletRequest, httpServletResponse, model);
        }

        String path = pathFactory.get(httpServletRequest, httpServletResponse, model);
        if (StringUtils.isNotEmpty(path)) {
            if (log.isDebugEnabled())
                log.debug("路径:{}, 工厂:{}", httpServletRequest.getRequestURL(), path);
            return path;
        }

        if (StringUtils.isEmpty(spm)) {
            if (log.isDebugEnabled())
                log.debug("默认路径:{}", httpServletRequest.getRequestURL());
            return pageFactory.index(httpServletRequest, httpServletResponse, model);
        }
        SuperPositionModelEntity superPositionModelEntity = getSpm(httpServletRequest, spm);
        if (null != superPositionModelEntity && StringUtils.isNotEmpty(superPositionModelEntity.getResourceId()))
            return superPositionModelEntity.getResourceId();
        if (log.isDebugEnabled())
            log.debug("无效路径:{}, 返回:404", httpServletRequest.getRequestURL());
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    /**
     * 判断是否需要登录
     */
    public boolean needLogin(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return loginFactory.isNeed(httpServletRequest, httpServletResponse);
    }

    public SuperPositionModelEntity getSpm(HttpServletRequest httpServletRequest, String spm) {
        if (null == spm)
            return null;
        SuperPositionModelEntity superPositionModelEntity = models.get(spm);
        if (null == superPositionModelEntity) {
            superPositionModelEntity = getSpmInternal(spm);
            if (null != superPositionModelEntity) {
                if (null != httpServletRequest)
                    log(httpServletRequest, superPositionModelEntity);
                models.put(spm, superPositionModelEntity);
            }
        }
        return superPositionModelEntity;
    }

    public SuperPositionModelEntity getByResourceId(String resourceId) {
        if (spmListForResourceID.containsKey(resourceId))
            return spmListForResourceID.get(resourceId);
        return baseMapper.getByResourceId(resourceId);
    }

    public SuperPositionModelEntity getByUrlKey(String urlKey) {
        if (spmListForUrlKey.containsKey(urlKey))
            return spmListForUrlKey.get(urlKey);
        return baseMapper.getByUrlKey(urlKey);
    }

    /**
     * 获取View的地址
     */
    public String getViewByKey(String key) {
        SuperPositionModelEntity superPositionModelEntity = getByUrlKey(key);
        if (null == superPositionModelEntity)
            return "";
        if (isSpmMode()) {
            return ("/?spm=" + superPositionModelEntity.getSpmValue());
        } else
            return ("/" + superPositionModelEntity.getResourceId());
    }

    /**
     * 根据 key 获取URL包含域名在内的全路径，
     */
    public String getUrl(String key) {
        String view = getViewByKey(key);
        if (StringUtils.isEmpty(view))
            return "";
        String siteDomain = sysConfigService.getSiteDomain();
        if (StringUtils.isNotEmpty(siteDomain)) {
            if ((!siteDomain.startsWith("http://") || !siteDomain.startsWith("https://"))) {
                if (sysConfigService.isSsl()) {
                    siteDomain = "https://" + siteDomain;
                } else {
                    siteDomain = "http://" + siteDomain;
                }
            }
        } else {
            siteDomain = "";
        }
        return siteDomain + view;
    }

    /**
     * 记录Spm 访问的日志信息
     */
    public void log(HttpServletRequest request, SuperPositionModelEntity superPositionModelEntity) {
        asyncTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                visitLogService.put(request, superPositionModelEntity);
            }
        });
    }

    private SuperPositionModelEntity getSpmInternal(String spm) {
        if (StringUtils.isEmpty(spm))
            return null;
        String[] ar = spm.split("\\.");
        Map<String, Object> map = new HashMap<>();
        if (ar.length > 0)
            map.put("site_id", ar[0]);
        if (ar.length > 1)
            map.put("page_id", ar[1]);
        if (ar.length > 2)
            map.put("channel_id", ar[2]);
        if (ar.length > 3)
            map.put("product_id", ar[3]);

        List<SuperPositionModelEntity> list = listByMap(map);
        if (list.isEmpty()) {
            map.remove("product_id");
            list = listByMap(map);
        }
        if (list.isEmpty()) {
            map.remove("channel_id");
            list = listByMap(map);
        }
        if (list.isEmpty()) {
            map.remove("page_id");
            list = listByMap(map);
        }
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public void put(String siteId, String pageId, String channelId, String productId, String resourceId, String urlPath, String urlKey, boolean needLogin) {
        SuperPositionModelEntity superPositionModelEntity = new SuperPositionModelEntity();
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(siteId)) {
            map.put("site_id", siteId);
            superPositionModelEntity.setSiteId(siteId);
        }
        if (StringUtils.isNotEmpty(pageId)) {
            map.put("page_id", pageId);
            superPositionModelEntity.setPageId(pageId);
        }
        if (StringUtils.isNotEmpty(channelId)) {
            map.put("channel_id", channelId);
            superPositionModelEntity.setChannelId(channelId);
        }
        if (StringUtils.isNotEmpty(productId)) {
            map.put("product_id", productId);
            superPositionModelEntity.setProductId(productId);
        }
        if (StringUtils.isNotEmpty(resourceId)) {
            map.put("resource_id", resourceId);
            superPositionModelEntity.setResourceId(resourceId);
        }
        if (StringUtils.isNotEmpty(urlPath)) {
            map.put("url_path", urlPath);
            superPositionModelEntity.setUrlPath(urlPath);
        }
        if (StringUtils.isNotEmpty(urlKey)) {
            map.put("url_key", urlKey);
            superPositionModelEntity.setUrlKey(urlKey);
        }
        superPositionModelEntity.setSpmValue(superPositionModelEntity.toSpmString());
        if (needLogin) {
            superPositionModelEntity.setNeedLogin(1);
        } else
            superPositionModelEntity.setNeedLogin(0);
        superPositionModelEntity.setForbidden(0);
        List<SuperPositionModelEntity> list = listByMap(map);
        if (!list.isEmpty()) {
            return;
        }
        save(superPositionModelEntity);
    }

    public boolean menuWithSpm() {
        return sysConfigService.getBoolean("MENU_WITH_SPM");
    }

    public boolean isSpmMode() {
        return menuWithSpm();
    }

    @Override
    public void onTenMinute() {
        load();
        models.clear();
    }

    @Override
    public boolean isNeed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String spm = httpServletRequest.getParameter("spm");
        if (null != spm) {
            SuperPositionModelEntity superPositionModelEntity = getSpm(httpServletRequest, spm);
            if (null != superPositionModelEntity) {
                if (null != superPositionModelEntity.getNeedLogin())
                    return superPositionModelEntity.getNeedLogin() > 0;
            }
        }
        if (isRoot(httpServletRequest))
            return true;
        String path = httpServletRequest.getRequestURI();
        if (null != path)
            path = path.substring(1);
        if (StringUtils.isNotBlank(path))
            return !mappingFactory.can(httpServletRequest, path);
        return true;
    }

    @Override
    public void clear() {
        models.clear();
    }
}
