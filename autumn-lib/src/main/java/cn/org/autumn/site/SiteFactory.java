package cn.org.autumn.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

@Component
public final class SiteFactory {

    public interface Site {
        @NotNull String getId();

        @NotNull String getPack();

        default String getKey(String fileName) {
            if (StringUtils.isEmpty(fileName))
                return "";
            try {
                Field field = getClass().getField(fileName);
                if (null != field) {
                    PageAware aware = field.getAnnotation(PageAware.class);
                    if (null != aware) {
                        String page = field.getName();
                        if (StringUtils.isNotEmpty(aware.page()) && !"NULL".equalsIgnoreCase(aware.page()) && !"0".equalsIgnoreCase(aware.page()))
                            page = aware.page();
                        return getId() + "_" + page;
                    }
                }
            } catch (Exception e) {
            }
            return getId() + "_" + fileName;
        }
    }

    public Collection<Site> getSites() {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return null;
        Map<String, Site> map = applicationContext.getBeansOfType(Site.class);
        return map.values();
    }
}