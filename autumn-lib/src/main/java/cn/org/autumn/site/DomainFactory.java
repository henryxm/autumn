package cn.org.autumn.site;

import cn.org.autumn.config.DomainHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DomainFactory extends Factory {

    List<DomainHandler> list = null;

    static final List<String> cache = new ArrayList<>();

    public boolean isSiteDomain(String domain) {
        if (null == list)
            list = getOrderList(DomainHandler.class);
        for (DomainHandler domainHandler : list) {
            boolean is = domainHandler.isSiteDomain(domain);
            if (is)
                return true;
        }
        return false;
    }

    public boolean isBindDomain(String domain) {
        if (null == list)
            list = getOrderList(DomainHandler.class);
        for (DomainHandler domainHandler : list) {
            boolean is = domainHandler.isBindDomain(domain);
            if (is)
                return true;
        }
        return false;
    }

    public synchronized boolean isSiteBind(String domain) {
        if (StringUtils.isBlank(domain))
            return false;
        domain = domain.trim().toLowerCase();
        if (cache.contains(domain))
            return true;
        boolean is = isSiteDomain(domain);
        if (is) {
            cache.add(domain);
            return true;
        }
        is = isBindDomain(domain);
        if (is) {
            cache.add(domain);
            return true;
        }
        return false;
    }

    public void clear() {
        cache.clear();
    }
}
