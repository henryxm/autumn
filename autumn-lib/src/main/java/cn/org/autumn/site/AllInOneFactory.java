package cn.org.autumn.site;

import cn.org.autumn.config.AllInOneHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AllInOneFactory extends Factory {

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
