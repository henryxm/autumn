package cn.org.autumn.site;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UpgradeFactory extends Factory {

    private boolean done = false;

    private static Map<String, Object> data = new HashMap<>();

    public static final String domainChanged = "domainChanged";

    //在初始化数据前执行
    public interface Upgrade {
        @Order(DEFAULT_ORDER)
        void upgrade();
    }

    public interface Domain {
        @Order(DEFAULT_ORDER)
        void onDomainChanged();
    }

    public static Map<String, Object> getData() {
        return data;
    }

    //call this method before init or load data
    public static void fireDomainChanged() {
        data.put(domainChanged, true);
    }

    public static boolean isDomainChanged() {
        return data.containsKey(domainChanged);
    }

    public boolean isDone() {
        return done;
    }

    public void upgrade() {
        if (isDomainChanged())
            invoke(UpgradeFactory.Domain.class, "onDomainChanged");
        invoke(UpgradeFactory.Upgrade.class, "upgrade");
        done = true;
    }
}