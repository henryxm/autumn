package cn.org.autumn.modules.spm.service;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.config.PostLoad;
import cn.org.autumn.config.PostLoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.service.gen.SuperPositionModelServiceGen;
import cn.org.autumn.modules.spm.site.SpmSite;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.site.LoginFactory;
import cn.org.autumn.site.SiteFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.*;

@Service
public class SuperPositionModelService extends SuperPositionModelServiceGen implements PostLoad, LoopJob.Job, LoginFactory.Login {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, SuperPositionModelEntity> sMap = new HashMap<>();

    @Autowired
    AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    PostLoadFactory postLoadFactory;

    @Autowired
    VisitLogService visitLogService;

    @Autowired
    SiteFactory siteFactory;

    @Autowired
    LoginFactory loginFactory;

    private static Map<String, String> spmListForHtml;
    private static Map<String, SuperPositionModelEntity> spmListForUrlKey;
    private static Map<String, SuperPositionModelEntity> spmListForResourceID;

    static {
        spmListForHtml = new LinkedHashMap<>();
        spmListForUrlKey = new LinkedHashMap<>();
        spmListForResourceID = new LinkedHashMap<>();
    }

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
            spmListForHtml.put(superPositionModelEntity.getUrlKey(), "spm=" + superPositionModelEntity.toString());
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
                String pack = site.getPack();
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

                boolean login = aware.login();
                String key = pack + "_" + page;
                put(siteId, page, channel, product, resource, url, key, login);
            }
        }
    }

    public String getResourceId(HttpServletRequest httpServletRequest, String spm) {
        if (StringUtils.isEmpty(spm)) {
            return "index";
        }
        String sessionId = httpServletRequest.getSession().getId();
        SuperPositionModelEntity superPositionModelEntity = sMap.get(sessionId);

        if (null == superPositionModelEntity)
            superPositionModelEntity = getSpm(httpServletRequest, spm);
        else
            sMap.remove(sessionId);
        if (null != superPositionModelEntity && StringUtils.isNotEmpty(superPositionModelEntity.getResourceId()))
            return superPositionModelEntity.getResourceId();
        return "index";
    }

    public boolean needLogin(HttpServletRequest httpServletRequest) {
        return loginFactory.isNeed(httpServletRequest);
    }

    public SuperPositionModelEntity getSpm(HttpServletRequest httpServletRequest, String spm) {
        SuperPositionModelEntity superPositionModelEntity = getSpmInternal(spm);
        if (null != superPositionModelEntity)
            log(httpServletRequest, superPositionModelEntity);
        return superPositionModelEntity;
    }

    public SuperPositionModelEntity getByResourceId(String resourceId) {
        if (null != spmListForResourceID && spmListForResourceID.containsKey(resourceId))
            return spmListForResourceID.get(resourceId);
        return baseMapper.getByResourceId(resourceId);
    }

    public SuperPositionModelEntity getByUrlKey(String urlKey) {
        if (null != spmListForUrlKey && spmListForUrlKey.containsKey(urlKey))
            return spmListForUrlKey.get(urlKey);
        return baseMapper.getByUrlKey(urlKey);
    }

    public String getViewByKey(String key) {
        SuperPositionModelEntity superPositionModelEntity = getByUrlKey(key);
        if (isSpmMode()) {
            return ("/?spm=" + superPositionModelEntity.getSpmValue());
        } else
            return ("/" + superPositionModelEntity.getResourceId());
    }

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

        String stamp = "";

        List<SuperPositionModelEntity> list = selectByMap(map);
        if (list.size() == 0) {
            map.remove("product_id");
            list = selectByMap(map);
        }
        if (list.size() == 0) {
            map.remove("channel_id");
            list = selectByMap(map);
        }
        if (list.size() == 0) {
            map.remove("page_id");
            list = selectByMap(map);
        }
        if (list.size() > 0) {
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
        superPositionModelEntity.setSpmValue(superPositionModelEntity.toString());
        if (needLogin) {
            superPositionModelEntity.setNeedLogin(1);
        } else
            superPositionModelEntity.setNeedLogin(0);
        superPositionModelEntity.setForbidden(0);
        List<SuperPositionModelEntity> list = selectByMap(map);
        if (list.size() > 0) {
            return;
        }
        insert(superPositionModelEntity);
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("spm_superpositionmodel_table_comment", "超级位置模型", "Super Position Model");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_id", "id");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_site_id", "网站ID", "Site ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_page_id", "网页ID", "Page ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_channel_id", "频道ID", "Channel ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_product_id", "产品ID", "Product ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_resource_id", "资源ID", "Resource ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_need_login", "需要登录", "Need login");
        super.addLanguageColumnItem();
    }

    public void init() {
        super.init();
        postLoadFactory.register(this);
        LoopJob.onOneMinute(this);
    }

    public boolean menuWithSpm() {
        return sysConfigService.getBoolean("MENU_WITH_SPM");
    }

    public boolean isSpmMode() {
        return menuWithSpm();
    }

    @Override
    public void runJob() {
        load();
    }

    @Override
    public boolean isNeed(HttpServletRequest httpServletRequest) {
        String sessionId = httpServletRequest.getSession().getId();
        String spm = httpServletRequest.getParameter("spm");
        SuperPositionModelEntity superPositionModelEntity = getSpm(httpServletRequest, spm);
        if (null != superPositionModelEntity) {
            sMap.put(sessionId, superPositionModelEntity);
            if (null != superPositionModelEntity.getNeedLogin())
                return superPositionModelEntity.getNeedLogin() > 0;
        }
        return true;
    }
}
