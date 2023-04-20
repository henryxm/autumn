package cn.org.autumn.site;

import cn.org.autumn.config.AllInOneHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AllInOneFactory extends Factory {

    Logger log = LoggerFactory.getLogger(getClass());

    List<AllInOneHandler> list = null;

    public boolean is() {
        try {
            if (null == list)
                list = getOrderList(AllInOneHandler.class);
            for (AllInOneHandler handler : list) {
                boolean is = handler.is();
                if (is)
                    return true;
            }
        } catch (Exception e) {
            log.error("All in one:", e);
        }
        return false;
    }
}
