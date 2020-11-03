package cn.org.autumn.site;

import cn.org.autumn.utils.SpringContextUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Map;

@Component
public final class SiteFactory {

    public interface Site {
        @NotNull String getId();

        @NotNull String getPack();
    }

    public Collection<Site> getSites() {
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (null == applicationContext)
            return null;
        Map<String, Site> map = applicationContext.getBeansOfType(Site.class);
        return map.values();
    }
}