package cn.org.autumn.modules.spm.service;

import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.service.gen.SuperPositionModelServiceGen;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SuperPositionModelService extends SuperPositionModelServiceGen {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, SuperPositionModelEntity> sMap = new HashMap<>();

    @Autowired
    AsyncTaskExecutor asyncTaskExecutor;

    @Override
    public int menuOrder() {
        return 7;
    }

    @Override
    public String ico() {
        return "fa-location-arrow";
    }

    public String getResourceId(HttpServletRequest httpServletRequest, String spm) {
        if (StringUtils.isEmpty(spm)) {
            return "index";
        }
        String sessionId = httpServletRequest.getSession().getId();
        SuperPositionModelEntity superPositionModelEntity = sMap.get(sessionId);

        if (null == superPositionModelEntity)
            superPositionModelEntity = getSpm(spm);
        else
            sMap.remove(sessionId);
        if (null != superPositionModelEntity && StringUtils.isNotEmpty(superPositionModelEntity.getResourceId()))
            return superPositionModelEntity.getResourceId();
        return "index";
    }

    /**
     * @param spm
     * @return
     */
    public boolean needLogin(HttpServletRequest httpServletRequest, String spm) {
        String sessionId = httpServletRequest.getSession().getId();
        SuperPositionModelEntity superPositionModelEntity = getSpm(spm);
        if (null != superPositionModelEntity) {
            sMap.put(sessionId, superPositionModelEntity);
            if (null != superPositionModelEntity.getNeedLogin())
                return superPositionModelEntity.getNeedLogin() > 0;
        }
        return true;
    }

    public SuperPositionModelEntity getSpm(String spm) {
        SuperPositionModelEntity superPositionModelEntity = getSpmInternal(spm);
        if (null != superPositionModelEntity)
            log(superPositionModelEntity);
        return superPositionModelEntity;
    }

    public void log(SuperPositionModelEntity superPositionModelEntity) {
        asyncTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                logger.info(superPositionModelEntity.toString());
            }
        });
    }

    private SuperPositionModelEntity getSpmInternal(String spm) {
        if (StringUtils.isEmpty(spm))
            return null;
        String[] ar = spm.split("\\.");
        Map<String, Object> map = new HashMap<>();
        if (ar.length > 0)
            map.put("site_id" , ar[0]);
        if (ar.length > 1)
            map.put("page_id" , ar[1]);
        if (ar.length > 2)
            map.put("channel_id" , ar[2]);
        if (ar.length > 3)
            map.put("product_id" , ar[3]);

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


    public void put(String siteId, String pageId, String channelId, String productId, String resourceId, boolean needLogin) {
        SuperPositionModelEntity superPositionModelEntity = new SuperPositionModelEntity();
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(siteId)) {
            map.put("site_id" , siteId);
            superPositionModelEntity.setSiteId(siteId);
        }
        if (StringUtils.isNotEmpty(pageId)) {
            map.put("page_id" , pageId);
            superPositionModelEntity.setPageId(pageId);
        }
        if (StringUtils.isNotEmpty(channelId)) {
            map.put("channel_id" , channelId);
            superPositionModelEntity.setChannelId(channelId);
        }
        if (StringUtils.isNotEmpty(productId)) {
            map.put("product_id" , productId);
            superPositionModelEntity.setProductId(productId);
        }
        if (StringUtils.isNotEmpty(resourceId)) {
            map.put("resource_id" , resourceId);
            superPositionModelEntity.setResourceId(resourceId);
        }
        if (needLogin) {
            superPositionModelEntity.setNeedLogin(1);
        } else
            superPositionModelEntity.setNeedLogin(0);
        List<SuperPositionModelEntity> list = selectByMap(map);
        if (list.size() > 0) {
            return;
        }
        insert(superPositionModelEntity);
    }

    public void init() {
        super.init();
        put("202010" , "202010" , "main" , "" , "main" , false);
        put("202010" , "202010" , "index" , "" , "index" , true);
        put("202010" , "202010" , "test" , "" , "test" , false);
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("spm_superpositionmodel_table_comment" , "超级位置模型" , "Super Position Model");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_id" , "id");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_site_id" , "网站ID" , "Site ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_page_id" , "网页ID" , "Page ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_channel_id" , "频道ID" , "Channel ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_product_id" , "产品ID" , "Product ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_resource_id" , "资源ID" , "Resource ID");
        languageService.addLanguageColumnItem("spm_superpositionmodel_column_need_login" , "需要登录" , "Need login");
        super.addLanguageColumnItem();
    }
}
